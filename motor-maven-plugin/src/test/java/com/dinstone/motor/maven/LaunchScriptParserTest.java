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

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

public class LaunchScriptParserTest {

    @Test
    public void testToByteArray() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("a.b", "haha");
        properties.setProperty("ab", "heihei");
        properties.setProperty("a.b.c", "heiha");
        InputStream inputStream = new FileInputStream("src/test/resources/launch.script");
        LaunchScriptParser p = new LaunchScriptParser(inputStream, properties);

        System.out.println(p.getContent());
    }

}
