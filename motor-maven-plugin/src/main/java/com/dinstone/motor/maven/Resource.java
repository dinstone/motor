package com.dinstone.motor.maven;

import org.apache.maven.plugins.annotations.Parameter;

public class Resource extends FileSet {

    @Parameter(required = true)
    private String outputDir;

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

}
