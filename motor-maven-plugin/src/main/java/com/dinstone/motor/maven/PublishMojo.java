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
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

@Mojo(name = "publish", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class PublishMojo extends AbstractMojo {

    /**
     * The Maven project.
     * 
     * @since 1.0
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Maven project helper utils.
     * 
     * @since 1.0
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Directory containing the generated archive.
     * 
     * @since 1.0
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;

    /**
     * Name of the generated archive.
     * 
     * @since 1.0
     */
    @Parameter(defaultValue = "${project.build.finalName}", required = true)
    private String finalName;

    /**
     * Skip the execution.
     * 
     * @since 1.2
     */
    @Parameter(property = "motor.publish.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Classifier to add to the artifact generated. If given, the artifact will be attached with that classifier and the
     * main artifact will be deployed as the main artifact. If this is not given (default), it will replace the main
     * artifact and only the repackaged artifact will be deployed. Attaching the artifact allows to deploy it alongside
     * to the original one, see
     * <a href= "http://maven.apache.org/plugins/maven-deploy-plugin/examples/deploying-with-classifiers.html" > the
     * maven documentation for more details</a>.
     * 
     * @since 1.0
     */
    @Parameter
    private String classifier;

    /**
     * Attach the repackaged archive to be installed and deployed.
     * 
     * @since 1.4
     */
    @Parameter(defaultValue = "false")
    private boolean attach = false;

    /**
     * Assembly the archiver include base directory
     */
    @Parameter(defaultValue = "true")
    private boolean includeBaseDirectory = true;

    /** Application configuration */
    @Parameter
    private Application application;

    /** Launcher configuration */
    @Parameter
    private Launcher launcher;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.project.getPackaging().equals("pom")) {
            getLog().debug("publish goal could not be applied to pom project.");
            return;
        }

        if (this.skip) {
            getLog().debug("skipping publish goal as per configuration.");
            return;
        }

        publish();
    }

    private void publish() throws MojoExecutionException {
        File source = this.project.getArtifact().getFile();
        getLog().info("Source file: " + source);
        File target = getTargetFile();
        getLog().info("Target file: " + target);

        try (ArchiverWriter aw = new ArchiverWriter(target, includeBaseDirectory, finalName)) {
            // bootstrap
            aw.writeLauncher();
            // script
            aw.writeScript(application, launcher);

            // lib
            Set<Artifact> libs = new HashSet<>();
            libs.add(this.project.getArtifact());
            libs.addAll(this.project.getArtifacts());
            aw.writeLibraries(libs);

            // config
            aw.writeConfig(application);

            // log
            aw.writeLog();

        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

        updateArtifact(source, target);
    }

    private void updateArtifact(File source, File repackaged) {
        if (this.attach) {
            String classifier = (this.classifier == null ? "" : this.classifier.trim());
            this.projectHelper.attachArtifact(this.project, "zip", classifier, repackaged);
        }
    }

    private File getTargetFile() {
        String classifier = (this.classifier == null ? "" : this.classifier.trim());
        if (classifier.length() > 0 && !classifier.startsWith("-")) {
            classifier = "-" + classifier;
        }
        if (!this.outputDirectory.exists()) {
            this.outputDirectory.mkdirs();
        }
        return new File(this.outputDirectory, this.finalName + classifier + ".zip");
    }

}