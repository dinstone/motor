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

import java.util.Properties;

import org.apache.maven.plugins.annotations.Parameter;

public class Launcher {

    /** launcher script files */
    @Parameter
    private Script script;

    @Parameter
    private Properties properties;

    public Script getScript() {
        return script;
    }

    public void setScript(Script script) {
        this.script = script;
    }

    /**
     * Properties that should be expanded in the launch script.
     * 
     * @return
     */
    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

}
