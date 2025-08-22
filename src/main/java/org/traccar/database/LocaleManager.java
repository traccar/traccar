/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.database;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Singleton
public class LocaleManager {

    private static final String DEFAULT_LANGUAGE = "en";

    private final Path path;
    private final ObjectMapper objectMapper;

    private final Map<String, Map<String, String>> languageBundles = new ConcurrentHashMap<>();

    @Inject
    public LocaleManager(Config config, ObjectMapper objectMapper) {
        path = Paths.get(config.getString(Keys.WEB_LOCALIZATION_PATH));
        this.objectMapper = objectMapper;
    }

    public Path getTemplateFile(String root, String path, String language, String fileName) {
        var languages = Stream.of(language, DEFAULT_LANGUAGE).filter(Objects::nonNull).toList();
        for (var targetLanguage : languages) {
            Path targetFile = Path.of(root, path, targetLanguage, fileName);
            if (Files.exists(targetFile)) {
                return Paths.get(path, targetLanguage, fileName);
            }
        }
        return null;
    }

    public Map<String, String> getBundle(String language) {
        String resolvedLanguage = language != null ? language : DEFAULT_LANGUAGE;
        return languageBundles.computeIfAbsent(resolvedLanguage, missingLanguage -> {
            Path targetFile = path.resolve(missingLanguage + ".json");
            Path file = Files.exists(targetFile) ? targetFile : path.resolve(DEFAULT_LANGUAGE + ".json");
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    return objectMapper.readValue(in, new TypeReference<>() { });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                return Collections.emptyMap();
            }
        });
    }

    public String getString(String language, String key) {
        return getBundle(language).get(key);
    }

}
