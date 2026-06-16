/*
 * Copyright (C) 2020-2026 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
 * which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
 * of the interface window, the display of the “You are using the Open Source and free version of LinID™, powered by
 * Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!” infobox and in the e-mails
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
import io.github.linagora.linid.im.api.i18n.collector.I18nMergeCollector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Loads i18n (internationalization) translation files embedded in plugin JAR
 * files.
 *
 * <p>
 * This loader scans a directory defined by the configuration property
 * {@code plugin.loader.directory}, looking for all JAR files.
 * It then attempts to extract and parse any translation files found within the
 * JARs that match the pattern
 * {@code i18n/{lang}.json} (e.g., {@code i18n/en.json}, {@code i18n/fr.json}).
 *
 * <p>
 * Translation files are expected to be simple JSON files containing key/value
 * pairs. The language code is derived from the
 * filename. All translations are merged into a final result map, grouped by
 * language.
 *
 * <p>
 * This loader supports the type {@code "plugin"}.
 */
@Slf4j
@Component
public class PluginI18nLoader implements I18nSourceLoader {
    /**
     * Pattern used to identify i18n JSON translation files within a JAR.
     */
    private final Pattern pattern = Pattern.compile("i18n/(.+)\\.json");

    /**
     * ObjectMapper for parsing JSON translation files.
     */
    private final ObjectMapper objectMapper;
    /**
     * Directory containing plugin JAR files. This value is injected from the
     * {@code plugin.loader.directory} configuration
     * property.
     */
    @Value("${plugin.loader.path}")
    private String pluginDirectoryPath;

    /**
     * Creates a new {@code PluginI18nLoader}.
     */
    public PluginI18nLoader() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean supports(final @NonNull String type) {
        return "plugin".equals(type);
    }

    @Override
    public Map<String, Map<String, String>> load() {
        Map<String, Map<String, String>> result = new HashMap<>();

        if (StringUtils.isBlank(pluginDirectoryPath)) {
            log.warn("PLUGIN_LOADER_PATH is not defined — no plugin translations will be loaded.");
            return result;
        }

        try (Stream<Path> paths = Files.walk(Paths.get(pluginDirectoryPath), 1)) {
            paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".jar"))
                .forEach(path -> {
                    try (JarFile jarFile = new JarFile(path.toFile())) {
                        var jarTranslations = parse(jarFile);
                        I18nMergeCollector.merge(result, jarTranslations);
                    } catch (IOException e) {
                        log.error("Error during loading translation from jar: {}", path, e);
                    }
                });
        } catch (Exception e) {
            log.error("Error walking through plugin directory: {}", pluginDirectoryPath, e);
        }

        return result;
    }

    /**
     * Parses the i18n JSON files contained within the provided {@link JarFile}.
     *
     * <p>
     * This method scans all entries in the JAR, filters for regular (non-directory)
     * entries whose path matches the pattern
     * {@code i18n/{lang}.json}, and attempts to parse them into a map of
     * translations.
     *
     * @param jarFile the JAR file to parse for i18n translation entries
     * @return a map where the keys are language codes (e.g., "en", "fr") and the
     * values are maps of translation keys to their
     * corresponding values. If a JAR entry is not a valid i18n file or
     * parsing fails, it is silently ignored.
     */
    public Map<String, Map<String, String>> parse(final JarFile jarFile) {
        return jarFile.stream()
            .filter(jarEntry -> !jarEntry.isDirectory())
            .map((jarEntry -> {
                Matcher matcher = pattern.matcher(jarEntry.getName());

                if (!matcher.matches()) {
                    return null;
                }

                try {
                    var lang = matcher.group(1);
                    var translations = objectMapper.readValue(
                        jarFile.getInputStream(jarEntry),
                        new TypeReference<Map<String, String>>() {
                        });

                    return Map.of(lang, translations);
                } catch (Exception e) {
                    return null;
                }
            }))
            .filter(Objects::nonNull)
            .collect(I18nMergeCollector.toCollect());
    }
}
