/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.weaver;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.classfile.Attributes;
import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleTypeAnnotationsAttribute;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.instruction.LabelTarget;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandleInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.dellroad.typetags.core.TypeTag;
import org.dellroad.typetags.core.TypeTagRuntime;

/**
 * Weaves runtime checks for TypeTags.
 *
 * <p>
 * Instances are thread safe.
 */
public class TypeTagsWeaver {

    private final Config config;
    private final ClassFile classFile;

// Constructor

    /**
     * Constructor.
     *
     * @param config configuration
     * @throws IllegalArgumentException if {@code config} is null
     */
    @SuppressWarnings("this-escape")
    public TypeTagsWeaver(Config config) {

        // Record our config
        if (config == null)
            throw new IllegalArgumentException("null config");
        this.config = config;

        // Initialize our ClassFile instance
        final ArrayList<ClassFile.Option> options = new ArrayList<>();
        if (this.config.loader() != null)
            options.add(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofClassLoading(this.config.loader())));
        this.classFile = ClassFile.of(options.toArray(new ClassFile.Option[0]));
    }

// BytecodeValidationWeaver

    // This augments CAST and INSTANCEOF instructions with a callout to the appropriate TypeTagValidator
    private class BytecodeValidationWeaver implements CodeTransform {

        // This caches the determinations as to which annotations should be woven
        private final Map<Class<? extends Annotation>, Boolean> shouldWeaveMap = new HashMap<>();

        // This records which points in the bytecode that should be woven and how
        private final Map<Label, WeaveAround> weaveAroundMap = new HashMap<>();

        // The WeaveAround and associated Label ready to apply to the next instruction we encounter
        private WeaveAround nextWeaveAround;
        private Label nextLabel;

        BytecodeValidationWeaver(MethodModel method) {

            // Gather all TypeAnnotation's
            final Stream<TypeAnnotation> visibleTypeAnnotations = method.code()
              .flatMap(code -> code.findAttribute(Attributes.runtimeVisibleTypeAnnotations()))
              .map(RuntimeVisibleTypeAnnotationsAttribute::annotations)
              .stream()
              .flatMap(List::stream);
            final Stream<TypeAnnotation> invisibleTypeAnnotations = method.code()
              .flatMap(code -> code.findAttribute(Attributes.runtimeInvisibleTypeAnnotations()))
              .map(RuntimeInvisibleTypeAnnotationsAttribute::annotations)
              .stream()
              .flatMap(List::stream);

            // Build mapping from @TypeTag-annotated CAST and INSTANCEOF instructions to corresponding WeaveAround's
            Stream.concat(visibleTypeAnnotations, invisibleTypeAnnotations)
              .filter(ta ->
                switch (ta.targetInfo().targetType()) {     // eagerly filter out types we don't support yet
                    case CAST, INSTANCEOF -> true;
                    default -> false;
                }
              ).forEach(ta -> {

                // Get the annotation and see if we should weave it
                final String className = ta.annotation().classSymbol().descriptorString()
                  .replaceAll("^L(.*);$", "$1")
                  .replace('/', '.');
                final Class<?> type;
                try {
                    type = TypeTagsWeaver.this.config.loader().loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(String.format("unable to load class \"%s\"", className), e);
                }
                final Class<? extends Annotation> annotationType = type.asSubclass(Annotation.class);
                if (!this.shouldWeave(annotationType))
                    return;

                // Get the target instruction's location
                final TypeAnnotation.TargetInfo targetInfo = ta.targetInfo();
                final TypeAnnotation.TargetType targetType = targetInfo.targetType();
                final Label label = switch (targetType) {
                    case INSTANCEOF -> ((TypeAnnotation.OffsetTarget)targetInfo).target();
                    case CAST -> ((TypeAnnotation.TypeArgumentTarget)targetInfo).target();
                    default -> throw new RuntimeException("unexpected");
                };

                // Add an AroundWeaver at the target instruction
                this.weaveAroundMap.put(label, switch (targetType) {
                    case INSTANCEOF -> new InstanceofWeaveAround(annotationType);
                    case CAST -> new CastWeaveAround(annotationType);
                    default -> throw new RuntimeException("unexpected");
                });
              });
        }

        private synchronized boolean shouldWeave(Class<? extends Annotation> annotationType0) {
            return this.shouldWeaveMap.computeIfAbsent(annotationType0, annotationType -> {
                final TypeTag typeTag = annotationType.getAnnotation(TypeTag.class);
                if (typeTag == null)
                    return false;
                if (TypeTagsWeaver.this.config.filter() != null && !TypeTagsWeaver.this.config.filter().test(annotationType))
                    return false;
                return true;
            });
        }

        @Override
        public void accept(CodeBuilder builder, CodeElement element) {
            switch (element) {
            case LabelTarget target when this.weaveAroundMap.containsKey(target.label()) -> {
                final Label label = target.label();
                final WeaveAround weaveAround = this.weaveAroundMap.get(label);
                if (this.nextWeaveAround != null)
                    throw new RuntimeException("unexpected duplicate label");
                this.nextWeaveAround = weaveAround;
                this.nextLabel = label;
            }
            case Instruction instruction when this.nextWeaveAround != null -> {
                this.nextWeaveAround.apply(builder, this.nextLabel, instruction);
                this.nextWeaveAround = null;
                this.nextLabel = null;
            }
            default -> builder.with(element);
            }
        }

    // WeaveAround classes

        private abstract class WeaveAround {

            final Class<? extends Annotation> annotationType;

            WeaveAround(Class<? extends Annotation> annotationType) {
                this.annotationType = annotationType;
            }

            void invoke(CodeBuilder codeBuilder, String methodName) {
                final ConstantPoolBuilder poolBuilder = codeBuilder.constantPool();
                final NameAndTypeEntry nameAndTypeEntry = switch (methodName) {
                case TypeTagRuntime.VALIDATE_METHOD_NAME ->
                  poolBuilder.nameAndTypeEntry(methodName, MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object));
                case TypeTagRuntime.IS_VALID_METHOD_NAME ->
                  poolBuilder.nameAndTypeEntry(methodName, MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_Object));
                default -> throw new RuntimeException("unexpected");
                };
                codeBuilder.invokedynamic(poolBuilder.invokeDynamicEntry(this.bsmEntry(poolBuilder), nameAndTypeEntry));
            }

            BootstrapMethodEntry bsmEntry(ConstantPoolBuilder builder) {
                return builder.bsmEntry(
                  builder.methodHandleEntry(
                    MethodHandleInfo.REF_invokeStatic,
                    builder.methodRefEntry(
                      builder.classEntry(ClassDesc.of(TypeTagRuntime.class.getName())),
                      builder.nameAndTypeEntry(
                        "bootstrap",
                        MethodTypeDesc.of(
                          ConstantDescs.CD_CallSite,
                          ConstantDescs.CD_MethodHandles_Lookup,
                          ConstantDescs.CD_String,
                          ConstantDescs.CD_MethodType,
                          ConstantDescs.CD_Class)))),
                  List.of(builder.classEntry(ClassDesc.of(this.annotationType.getName()))));
            }

            abstract void apply(CodeBuilder builder, Label label, Instruction instruction);
        }

        private class CastWeaveAround extends WeaveAround {

            CastWeaveAround(Class<? extends Annotation> annotationType) {
                super(annotationType);
            }

            @Override
            void apply(CodeBuilder builder, Label label, Instruction instruction) {

                // Emit the original label
                builder.labelBinding(label);

                // Invoke TypeTagValidator.validate()
                builder.dup();
                this.invoke(builder, TypeTagRuntime.VALIDATE_METHOD_NAME);

                // Emit the instruction
                builder.with(instruction);
            }
        }

        private class InstanceofWeaveAround extends WeaveAround {

            InstanceofWeaveAround(Class<? extends Annotation> annotationType) {
                super(annotationType);
            }

            @Override
            void apply(CodeBuilder builder, Label label, Instruction instruction) {

                // Sanity check
                if (instruction.opcode() != Opcode.INSTANCEOF)
                    throw new RuntimeException("unexpected instruction: " + instruction);

                // Emit the original label
                builder.labelBinding(label);

                // Duplicate top value
                builder.dup();

                // Perform the original INSTANCEOF
                builder.with(instruction);

                // Move value back to top of stack
                builder.swap();

                // Invoke TypeTagValidator.isValid()
                this.invoke(builder, TypeTagRuntime.IS_VALID_METHOD_NAME);

                // OR the two results together
                builder.iand();
            }
        }
    }

    /**
     * Get the {@link Config} associated with this instance.
     *
     * @return instance config
     */
    public Config getConfig() {
        return this.config;
    }

// Weaving

    /**
     * Weave runtime checks into the specified class file.
     *
     * @throws IllegalArgumentException if {@code path} is null
     */
    public void addRuntimeChecks(Path path) throws IOException {

        // Read classfile
        if (path == null)
            throw new IllegalArgumentException("null path");

        // Transform classfile
        final ClassModel input = this.classFile.parse(path);
        final byte[] output = this.classFile.transformClass(input, (classBuilder, classElement) -> {
            switch (classElement) {
            case MethodModel method when method.code().isPresent() -> {
                final BytecodeValidationWeaver weaver = new BytecodeValidationWeaver(method);
                classBuilder.withMethodBody(method.methodName(), method.methodType(), method.flags().flagsMask(),
                  codeBuilder -> method.code().get().forEach(codeElement -> weaver.accept(codeBuilder, codeElement)));
            }
            default -> classBuilder.with(classElement);
            }
        });

        // Overwrite file
        Files.write(path, output);
    }
}
