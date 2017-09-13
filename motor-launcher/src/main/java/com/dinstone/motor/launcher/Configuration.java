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

package com.dinstone.motor.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class Configuration {

    public static final String LAUNCHER_HOME_TOKEN = "${launcher.home}";

    public static final String APPLICATION_HOME_TOKEN = "${application.home}";

    private static final String LAUNCHER_HOME = "launcher.home";

    private static final String APPLICATION_HOME = "application.home";

    private Properties properties = new Properties();

    public Configuration() {
        loadConfig();
    }

    private void loadConfig() {
        // first find launcher file from system property
        InputStream is = null;
        try {
            String configUrl = System.getProperty("launcher.config");
            if (configUrl != null) {
                is = (new URL(configUrl)).openStream();
            }
        } catch (Throwable t) {
        }

        // second find launcher file from launcher home's 'bin' dir
        if (is == null) {
            try {
                String launcherHome = getLauncherHome();
                File confidDir = new File(launcherHome, "bin");
                File configFile = new File(confidDir, "launcher.properties");
                is = new FileInputStream(configFile);
            } catch (Throwable t) {
            }
        }

        if (is != null) {
            try {
                loadProperties(is);
            } catch (Throwable t) {
                // LOG.log(Level.WARNING, "Failed to load launcher.properties.", t);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Load properties.
     * 
     * @throws IOException
     */
    private void loadProperties(InputStream is) throws IOException {
        properties.load(is);
    }

    /**
     * Set the <code>launcher.home</code> System property to the current working directory if it has not been set.
     * 
     * @return
     */
    public String getLauncherHome() {
        // check system property
        String launcherHome = System.getProperty(LAUNCHER_HOME);
        if (launcherHome != null) {
            return launcherHome;
        }

        // create launcher.home
        String userDir = System.getProperty("user.dir");
        File bootstrap = new File(userDir, "bootstrap.jar");
        if (bootstrap.exists()) {
            try {
                File parentDir = new File(userDir, "..");
                System.setProperty(LAUNCHER_HOME, parentDir.getCanonicalPath());
            } catch (Exception e) {
                // set launcher.home with user.dir
                System.setProperty(LAUNCHER_HOME, userDir);
            }
        } else {
            // set launcher.home with user.dir
            System.setProperty(LAUNCHER_HOME, userDir);
        }

        return System.getProperty(LAUNCHER_HOME);
    }

    public String getApplicationHome() {
        // find applicationHome from system properties
        String applicationHome = System.getProperty(APPLICATION_HOME);
        if (applicationHome != null && applicationHome.length() > 0) {
            return applicationHome;
        }

        // find applicationHome from launcher config
        applicationHome = properties.getProperty(APPLICATION_HOME);
        if (applicationHome != null && applicationHome.length() > 0) {
            applicationHome = fillPlaceholder(applicationHome, getLauncherHome(), LAUNCHER_HOME_TOKEN);
            System.setProperty(APPLICATION_HOME, applicationHome);
            return applicationHome;
        }

        // default setting launcher home with launcher home
        applicationHome = getLauncherHome();
        System.setProperty(APPLICATION_HOME, applicationHome);
        return applicationHome;
    }

    public static String fillPlaceholder(String source, String replace, String partten) {
        int index = source.indexOf(partten);
        if (index == 0) {
            source = replace + source.substring(partten.length());
        }
        return source;
    }

    public String getProperty(String name) {
        String v = properties.getProperty(name);
        if (v == null) {
            v = System.getProperty(name);
        }
        return v;
    }

    /**
     * Return specified property value.
     */
    public String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    /**
     * @param name
     * @param value
     */
    public void setProperty(String name, String value) {
        properties.setProperty(name, value);
    }

    @Override
    public String toString() {
        return properties.toString();
    }

}
