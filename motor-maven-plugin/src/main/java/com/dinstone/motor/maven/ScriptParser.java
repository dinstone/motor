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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptParser {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([\\w,.]+)(:.*?)?\\}\\}(?!\\})");

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final int BUFFER_SIZE = 4096;

    private final String content;

    public ScriptParser(InputStream inputStream, Properties properties) throws IOException {
        String content = loadContent(inputStream);
        this.content = expandPlaceholders(content, properties);
    }

    private String loadContent(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        copy(inputStream, outputStream);
        return new String(outputStream.toByteArray(), UTF_8);
    }

    private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
    }

    private String expandPlaceholders(String content, Map<?, ?> properties) {
        StringBuffer expanded = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = matcher.group(2);
            if (properties != null && properties.containsKey(name)) {
                value = (String) properties.get(name);
            } else {
                value = (value == null ? matcher.group(0) : value.substring(1));
            }
            matcher.appendReplacement(expanded, value.replace("$", "\\$"));
        }
        matcher.appendTail(expanded);
        return expanded.toString();
    }

    public byte[] toByteArray() {
        return content.getBytes(UTF_8);
    }

    public String getContent() {
        return content;
    }

}
