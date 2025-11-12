/*
 * Copyright (C) 2025 Archie L. Cobbs. All rights reserved.
 */

package org.dellroad.typetags.maven;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.dellroad.typetags.weaver.Config;
import org.dellroad.typetags.weaver.TypeTagsWeaver;

@Mojo(name = "typetags-weaver",
  defaultPhase = LifecyclePhase.PROCESS_CLASSES,
  requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
  threadSafe = true)
public class TypeTagsWeaverMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
    protected File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // Set up class loader that includes project classes and dependencies
        final TreeSet<URL> urls = new TreeSet<>(new Comparator<URL>() {                 // sort URLs to aid in debugging
            @Override
            public int compare(URL url1, URL url2) {
                return url1.toString().compareTo(url2.toString());
            }
        });
        try {
            urls.add(this.outputDirectory.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new MojoExecutionException(String.format("Error creating URL from directory \"%s\"", this.outputDirectory), e);
        }
        final ArrayList<String> dependencies = new ArrayList<>();
        try {
            this.addDependencyClasspathElements(dependencies);
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error gathering dependency classpath elements", e);
        }
        for (String dependency : dependencies) {
            try {
                urls.add(new File(dependency).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new MojoExecutionException(String.format(
                  "Error creating URL from classpath element \"%s\"", dependency), e);
            }
        }
        if (this.getLog().isDebugEnabled()) {
            this.getLog().debug(String.format("%s classloader setup:%n%s",
              this.getClass().getSimpleName(), this.listOfStrings(urls)));
        }

        // Find class files
        final ArrayList<Path> classFiles = new ArrayList<>();
        final Path dir = this.outputDirectory.toPath();
        try {
            Files.walkFileTree(this.outputDirectory.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    path = dir.relativize(path);
                    if (path.getNameCount() > 0 && path.getName(path.getNameCount() - 1).toString().endsWith(".class"))
                        classFiles.add(path);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new MojoExecutionException("Error walking output directory hierarchy", e);
        }
        if (this.getLog().isDebugEnabled()) {
            this.getLog().debug(String.format("%s class files found under %s:%n%s",
              this.getClass().getSimpleName(), this.outputDirectory, this.listOfStrings(classFiles)));
        }

        // Weave classes while providing a context class loader with access to all dependencies
        final ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
        final ClassLoader loader = URLClassLoader.newInstance(urls.toArray(new URL[urls.size()]), parentLoader);
        Thread.currentThread().setContextClassLoader(loader);
        try {
            this.weaveClasses(classFiles);
        } finally {
            Thread.currentThread().setContextClassLoader(parentLoader);
        }
    }

    @SuppressWarnings("unchecked")
    protected void addDependencyClasspathElements(List<String> elements) throws DependencyResolutionRequiredException {
        elements.addAll((List<String>)this.project.getCompileClasspathElements());
        elements.addAll((List<String>)this.project.getRuntimeClasspathElements());
    }

    protected void weaveClasses(List<Path> classFiles) throws MojoExecutionException, MojoFailureException {

        // Configure weaver
        final Config config = Config.newBuilder()
          .loader(Thread.currentThread().getContextClassLoader())
          .build();
        final TypeTagsWeaver weaver = new TypeTagsWeaver(config);

        // Process files in parallel
        try (ForkJoinPool pool = new ForkJoinPool()) {

            // Create tasks
            final Set<Callable<Void>> tasks = classFiles.stream()
              .map(path -> (Callable<Void>)() -> {
                final Path absolutePath = this.outputDirectory.toPath().resolve(path);
                if (this.getLog().isDebugEnabled())
                    this.getLog().debug(String.format("weaving file %s", absolutePath));
                weaver.addRuntimeChecks(absolutePath);
                return null;
              }).collect(Collectors.toSet());

            // Execute tasks
            final List<Future<Void>> futures;
            try {
                futures = pool.invokeAll(tasks);
            } catch (InterruptedException e) {
                throw new MojoExecutionException("interrupted", e);
            }
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    throw new MojoExecutionException("interrupted", e);
                } catch (ExecutionException e) {
                    throw new MojoExecutionException("weaving error", e);
                }
            }
        }
    }

    private String listOfStrings(Iterable<?> items) {
        final StringBuilder buf = new StringBuilder();
        for (Object item : items) {
            if (buf.length() > 0)
                buf.append("\n");
            buf.append("  ").append(item);
        }
        return buf.toString();
    }
}
