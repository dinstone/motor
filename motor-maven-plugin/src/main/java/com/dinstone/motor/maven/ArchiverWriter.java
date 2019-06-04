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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.utils.io.DirectoryScanner;

public class ArchiverWriter implements AutoCloseable {

    private static final String NESTED_LOADER_JAR = "META-INF/launcher/motor-launcher.jar";

    private static final String NESTED_SCRIPT_JAR = "META-INF/launcher/motor-scripts.jar";

    private static final int BUFFER_SIZE = 32 * 1024;

    private final JarArchiveOutputStream jarOutput;

    private final Set<String> writtenEntries = new HashSet<>();

    private String rootPrefix;

    public ArchiverWriter(File file, boolean includeBaseDirectory, String finalName)
            throws FileNotFoundException, IOException {
        if (includeBaseDirectory) {
            this.rootPrefix = finalName;
        } else {
            this.rootPrefix = "";
        }

        if (!"".equals(rootPrefix) && !rootPrefix.endsWith("/")) {
            this.rootPrefix += "/";
        }

        FileOutputStream fileOutputStream = new FileOutputStream(file);
        this.jarOutput = new JarArchiveOutputStream(fileOutputStream);
        this.jarOutput.setEncoding("UTF-8");
    }

    public void writeLauncher() throws IOException {
        URL loaderJar = getClass().getClassLoader().getResource(NESTED_LOADER_JAR);
        writeEntry(rootPrefix + "bin/bootstrap.jar", loaderJar.openStream());
    }

    public void writeScripts(Application application, Launcher launcher) throws IOException {
        if (launcher == null) {
            launcher = new Launcher();
            launcher.setProperties(new Properties());
            launcher.setScripts(new Script[0]);
        }

        Properties properties = launcher.getProperties();
        if (properties == null) {
            properties = new Properties();
        }

        if (application != null && application.getActivator() != null) {
            properties.put("application.activator", application.getActivator());
        }

        Script[] scripts = launcher.getScripts();
        if (scripts != null && scripts.length > 0) {
            for (Script script : scripts) {
                writeScript(properties, script);
            }
        } else {
            URL scriptsJar = getClass().getClassLoader().getResource(NESTED_SCRIPT_JAR);
            JarInputStream inputStream = new JarInputStream(new BufferedInputStream(scriptsJar.openStream()));
            JarEntry entry = null;
            while ((entry = inputStream.getNextJarEntry()) != null) {
                if (!entry.getName().startsWith("META-INF")) {
                    ScriptParser scriptParser = new ScriptParser(inputStream, properties);
                    writeEntry(rootPrefix + "bin/" + entry.getName(),
                            new ByteArrayInputStream(scriptParser.toByteArray()));
                }
            }
            inputStream.close();
        }
    }

    private void writeScript(Properties properties, Script script) throws IOException, FileNotFoundException {
        if (script != null && script.getDirectory() != null) {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(script.getDirectory());
            scanner.setIncludes(script.getIncludes());
            scanner.setExcludes(script.getExcludes());
            scanner.addDefaultExcludes();
            scanner.scan();

            String basePath = script.getDirectory().getAbsolutePath();
            String[] includeFiles = scanner.getIncludedFiles();
            for (String includeFile : includeFiles) {
                try (FileInputStream scriptFile = new FileInputStream(new File(basePath, includeFile))) {
                    ScriptParser scriptParser = new ScriptParser(scriptFile, properties);
                    writeEntry(rootPrefix + "bin/" + includeFile, new ByteArrayInputStream(scriptParser.toByteArray()));
                }
            }
        }
    }

    public void writeLibraries(Set<Artifact> artifacts) throws IOException {
        Set<String> seen = new HashSet<>();
        String destination = rootPrefix + "lib/";
        for (Artifact artifact : artifacts) {
            if (!seen.add(destination + artifact.getFile().getName())) {
                throw new IllegalStateException("Duplicate library " + artifact.getFile().getName());
            }
            writeLibrary(destination, artifact);
        }
    }

    public void writeConfigs(Application application) throws IOException {
        writeEntry(rootPrefix + "config/", null);
        if (application != null && application.getConfigs() != null) {
            Config[] configs = application.getConfigs();
            for (Config config : configs) {
                if (config.getDirectory() != null) {
                    DirectoryScanner scanner = new DirectoryScanner();
                    scanner.setBasedir(config.getDirectory());
                    scanner.setIncludes(config.getIncludes());
                    scanner.setExcludes(config.getExcludes());
                    scanner.addDefaultExcludes();
                    scanner.scan();

                    String basePath = config.getDirectory().getAbsolutePath();
                    String[] includeFiles = scanner.getIncludedFiles();
                    for (String includeFile : includeFiles) {
                        writeEntry(rootPrefix + "config/" + includeFile,
                                new FileInputStream(new File(basePath, includeFile)));
                    }
                }
            }
        }
    }

    public void writeResources(Application application) throws IOException {
        if (application != null && application.getResources() != null) {
            Resource[] resources = application.getResources();
            for (Resource resource : resources) {
                if (resource.getDirectory() != null && resource.getOutputDir() != null) {
                    DirectoryScanner scanner = new DirectoryScanner();
                    scanner.setBasedir(resource.getDirectory());
                    scanner.setIncludes(resource.getIncludes());
                    scanner.setExcludes(resource.getExcludes());
                    scanner.addDefaultExcludes();
                    scanner.scan();

                    String outputDir = resource.getOutputDir();
                    if (outputDir.startsWith("/")) {
                        outputDir = outputDir.substring(1);
                    }
                    if (outputDir.length() > 0 && !outputDir.endsWith("/")) {
                        outputDir += "/";
                    }
                    writeEntry(rootPrefix + outputDir, null);

                    String basePath = resource.getDirectory().getAbsolutePath();
                    String[] includeFiles = scanner.getIncludedFiles();
                    for (String includeFile : includeFiles) {
                        writeEntry(rootPrefix + outputDir + includeFile,
                                new FileInputStream(new File(basePath, includeFile)));
                    }
                }
            }
        }
    }

    public void writeLog() throws IOException {
        writeEntry(rootPrefix + "logs/", null);
    }

    /**
     * Close the writer.
     * 
     * @throws IOException
     *             if the file cannot be closed
     */
    @Override
    public void close() throws IOException {
        this.jarOutput.close();
    }

    private long getNestedLibraryTime(File file) {
        try {
            try (JarFile jarFile = new JarFile(file)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        return entry.getTime();
                    }
                }
            }
        } catch (Exception ex) {
            // Ignore and just use the source file timestamp
        }
        return file.lastModified();
    }

    private void writeLibrary(String destination, Artifact artifact) throws IOException {
        File file = artifact.getFile();
        JarArchiveEntry entry = new JarArchiveEntry(destination + file.getName());
        entry.setTime(getNestedLibraryTime(file));
        new CrcAndSize(file).setupStoredEntry(entry);
        writeEntry(entry, new InputStreamEntryWriter(new FileInputStream(file), true));
    }

    /**
     * Writes an entry. The {@code inputStream} is closed once the entry has been
     * written
     * 
     * @param entryName
     *            The name of the entry
     * @param inputStream
     *            The stream from which the entry's data can be read
     * @throws IOException
     *             if the write fails
     */
    private void writeEntry(String entryName, InputStream inputStream) throws IOException {
        JarArchiveEntry entry = new JarArchiveEntry(entryName);
        if (inputStream != null) {
            writeEntry(entry, new InputStreamEntryWriter(inputStream, true));
        } else {
            writeEntry(entry, null);
        }
    }

    /**
     * Perform the actual write of a {@link JarEntry}. All other {@code write}
     * method delegate to this one.
     * 
     * @param entry
     *            the entry to write
     * @param entryWriter
     *            the entry writer or {@code null} if there is no content
     * @throws IOException
     *             in case of I/O errors
     */
    private void writeEntry(JarArchiveEntry entry, EntryWriter entryWriter) throws IOException {
        String parent = entry.getName();
        if (parent.endsWith("/")) {
            parent = parent.substring(0, parent.length() - 1);
            entry.setUnixMode(UnixStat.DIR_FLAG | UnixStat.DEFAULT_DIR_PERM);
        } else if (parent.endsWith(".sh")) {
            entry.setUnixMode(UnixStat.FILE_FLAG | UnixStat.DEFAULT_DIR_PERM);
        } else {
            entry.setUnixMode(UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM);
        }

        if (parent.lastIndexOf("/") != -1) {
            parent = parent.substring(0, parent.lastIndexOf("/") + 1);
            if (parent.length() > 0) {
                writeEntry(new JarArchiveEntry(parent), null);
            }
        }

        if (this.writtenEntries.add(entry.getName())) {
            this.jarOutput.putArchiveEntry(entry);
            if (entryWriter != null) {
                entryWriter.write(this.jarOutput);
            }
            this.jarOutput.closeArchiveEntry();
        }
    }

    /**
     * Interface used to write jar entry date.
     */
    private interface EntryWriter {

        /**
         * Write entry data to the specified output stream.
         * 
         * @param outputStream
         *            the destination for the data
         * @throws IOException
         *             in case of I/O errors
         */
        void write(OutputStream outputStream) throws IOException;

    }

    /**
     * {@link EntryWriter} that writes content from an {@link InputStream}.
     */
    private static class InputStreamEntryWriter implements EntryWriter {

        private final InputStream inputStream;

        private final boolean close;

        InputStreamEntryWriter(InputStream inputStream, boolean close) {
            this.inputStream = inputStream;
            this.close = close;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            while ((bytesRead = this.inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            if (this.close) {
                this.inputStream.close();
            }
        }

    }

    /**
     * Data holder for CRC and Size.
     */
    private static class CrcAndSize {

        private final CRC32 crc = new CRC32();

        private long size;

        CrcAndSize(File file) throws IOException {
            try (FileInputStream inputStream = new FileInputStream(file)) {
                load(inputStream);
            }
        }

        private void load(InputStream inputStream) throws IOException {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                this.crc.update(buffer, 0, bytesRead);
                this.size += bytesRead;
            }
        }

        public void setupStoredEntry(JarArchiveEntry entry) {
            entry.setSize(this.size);
            entry.setCompressedSize(this.size);
            entry.setCrc(this.crc.getValue());
            entry.setMethod(ZipEntry.STORED);
        }

    }

}
