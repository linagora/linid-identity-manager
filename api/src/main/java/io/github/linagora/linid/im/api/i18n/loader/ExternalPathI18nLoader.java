/*
 * Copyright (C) 2020-2026 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
 * which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
 * of the interface window, the display of the "You are using the Open Source and free version of LinID™, powered by
 * Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!" infobox and in the e-mails
 * sent with the Program, notice appended to any type of outbound messages (e.g. e-mail and meeting requests) as well
 * as in the LinID Identity Manager user interface, (ii) retain all hypertext links between LinID Identity Manager
 * and https://linid.org/, as well as between LINAGORA and LINAGORA.com, and (iii) refrain from infringing LINAGORA
 * intellectual property rights over its trademarks and commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License and its applicable Additional Terms for
 * LinID Identity Manager along with this program. If not, see <http://www.gnu.org/licenses/> for the GNU Affero
 * General Public License version 3 and <http://www.linagora.com/licenses/> for the Additional Terms applicable to the
 * LinID Identity Manager software.
 */

package io.github.linagora.linid.im.api.i18n.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Loads i18n (internationalization) translation files from an external file
 * system path.
 *
 * <p>
 * This implementation scans a directory specified by the configuration property
 * {@code i18n.external.path} and loads all JSON
 * files that contain key/value translation pairs. Each file should be named
 * after the language code (e.g., {@code en.json},
 * {@code fr.json}).
 *
 * <p>
 * This loader supports the type {@code "external"}.
 */
@Slf4j
@Component
public class ExternalPathI18nLoader implements I18nSourceLoader {

    /**
     * Path to the directory on the file system containing i18n JSON files. This
     * value is injected from the
     * {@code i18n.external.path} configuration property.
     */
    @Value("${i18n.external.path:}")
    private String i18nExternalPath;

    /**
     * ObjectMapper for parsing JSON translation files.
     */
    private final ObjectMapper objectMapper;

    public ExternalPathI18nLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(@NonNull String type) {
        return "external".equals(type);
    }

    @Override
    public Map<String, Map<String, String>> load() {
        Map<String, Map<String, String>> result = new HashMap<>();

        if (i18nExternalPath == null || i18nExternalPath.isBlank()) {
            return result;
        }

        try (Stream<Path> paths = Files.walk(Paths.get(i18nExternalPath))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(this::parse)
                    .filter(Objects::nonNull)
                    .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        } catch (IOException e) {
            log.error("Error during loading all translations from path: {}", i18nExternalPath, e);
        }

        return result;
    }

    /**
     * Parses a single translation file from the given path into a map entry.
     *
     * @param path the path to the JSON file
     * @return a map entry where the key is the language code (derived from the
     *         filename), and the value is the map of translation
     *         keys and values; returns {@code null} if an error occurs while
     *         reading or parsing the file.
     */
    Map.Entry<String, Map<String, String>> parse(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            var lang = path.getFileName().toString().replace(".json", "");
            var translations = objectMapper.readValue(
                    inputStream,
                    new TypeReference<Map<String, String>>() {
                    });

            return Map.entry(lang, translations);
        } catch (IOException e) {
            log.error("Error during loading translation from path: {}", path, e);
        }

        return null;
    }
}
