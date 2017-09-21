/*
 * Copyright (C) 2014~2017 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dinstone.motor.maven;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Run an executable archive application.
 */
@Mojo(name = "run", requiresProject = true, defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class RunMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Skip the execution.
     */
    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    /** Application configuration */
    @Parameter
    private Application application;

    /**
     * Additional folders besides the classes directory that should be added to the classpath.
     */
    @Parameter
    private String[] folders;

    /**
     * Directory containing the classes and resource files that should be packaged into the archive.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File classesDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skip) {
            getLog().debug("skipping run as per configuration.");
            return;
        }
        run();
    }

    private void run() throws MojoExecutionException, MojoFailureException {
        if (application == null || application.getActivator() == null) {
            getLog().warn(
                "Application activator is invalid, please setting by <configuration><activator>x.y.Activator</activator></configuration>");
            return;
        }

        String activator = application.getActivator();
        IsolatedThreadGroup threadGroup = new IsolatedThreadGroup(activator);
        Thread launchThread = new Thread(threadGroup, new LaunchRunner(activator), "start");
        launchThread.setContextClassLoader(new URLClassLoader(getClassPathUrls()));
        launchThread.start();
        join(threadGroup);
        threadGroup.rethrowUncaughtException();
    }

    private void join(ThreadGroup threadGroup) {
        boolean hasNonDaemonThreads;
        do {
            hasNonDaemonThreads = false;
            Thread[] threads = new Thread[threadGroup.activeCount()];
            threadGroup.enumerate(threads);
            for (Thread thread : threads) {
                if (thread != null && !thread.isDaemon()) {
                    try {
                        hasNonDaemonThreads = true;
                        thread.join();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } while (hasNonDaemonThreads);
    }

    protected URL[] getClassPathUrls() throws MojoExecutionException {
        try {
            List<URL> urls = new ArrayList<URL>();
            addUserDefinedFolders(urls);
            addProjectClasses(urls);
            addDependencies(urls);
            return urls.toArray(new URL[urls.size()]);
        } catch (MalformedURLException ex) {
            throw new MojoExecutionException("Unable to build classpath", ex);
        }
    }

    private void addDependencies(List<URL> urls) throws MalformedURLException, MojoExecutionException {
        Set<Artifact> artifacts = this.project.getArtifacts();
        for (Artifact artifact : artifacts) {
            if (artifact.getFile() != null) {
                urls.add(artifact.getFile().toURI().toURL());
            }
        }
    }

    private void addProjectClasses(List<URL> urls) throws MalformedURLException {
        urls.add(this.classesDirectory.toURI().toURL());
    }

    private void addUserDefinedFolders(List<URL> urls) throws MalformedURLException {
        if (this.folders != null) {
            for (String folder : this.folders) {
                urls.add(new File(folder).toURI().toURL());
            }
        }
    }

    class IsolatedThreadGroup extends ThreadGroup {

        private final Object monitor = new Object();

        private Throwable exception;

        IsolatedThreadGroup(String name) {
            super(name);
        }

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            if (!(ex instanceof ThreadDeath)) {
                synchronized (this.monitor) {
                    this.exception = (this.exception == null ? ex : this.exception);
                }
                getLog().warn(ex);
            }
        }

        public void rethrowUncaughtException() throws MojoExecutionException {
            synchronized (this.monitor) {
                if (this.exception != null) {
                    throw new MojoExecutionException(
                        "An exception occurred while running. " + this.exception.getMessage(), this.exception);
                }
            }
        }

    }

    /**
     * Runner used to launch the application.
     */
    class LaunchRunner implements Runnable {

        private final String startClassName;

        LaunchRunner(String startClassName) {
            this.startClassName = startClassName;
        }

        @Override
        public void run() {
            Thread thread = Thread.currentThread();
            ClassLoader classLoader = thread.getContextClassLoader();
            try {
                Class<?> startClass = classLoader.loadClass(this.startClassName);
                Method mainMethod = startClass.getMethod("start", (Class[]) null);
                if (!mainMethod.isAccessible()) {
                    mainMethod.setAccessible(true);
                }
                // new an activator object
                Object activator = startClass.newInstance();
                mainMethod.invoke(activator, (Object[]) null);
            } catch (NoSuchMethodException ex) {
                Exception wrappedEx = new Exception("The specified activator doesn't contain a start method.", ex);
                thread.getThreadGroup().uncaughtException(thread, wrappedEx);
            } catch (Exception ex) {
                thread.getThreadGroup().uncaughtException(thread, ex);
            }
        }

    }

}
