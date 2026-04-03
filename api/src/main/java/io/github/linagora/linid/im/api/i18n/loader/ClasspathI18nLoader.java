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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Loads i18n (internationalization) translation files from the classpath.
 *
 * <p>
 * This implementation looks for JSON files in the
 * {@code classpath*:i18n/*.json} location, where each file is expected to be
 * named after the language code (e.g., {@code en.json}, {@code fr.json}) and to
 * contain a flat map of key/value translation
 * pairs.
 *
 * <p>
 * This loader supports the type {@code "internal"}.
 */
@Slf4j
@Component
public class ClasspathI18nLoader implements I18nSourceLoader {

    /**
     * Resolver used to scan and load resources from the classpath.
     */
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    /**
     * ObjectMapper for parsing JSON translation files.
     */
    private final ObjectMapper objectMapper;

    public ClasspathI18nLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(@NonNull String type) {
        return "internal".equals(type);
    }

    @Override
    public Map<String, Map<String, String>> load() {
        Map<String, Map<String, String>> result = new HashMap<>();

        try {
            for (var resource : resolver.getResources("classpath*:i18n/*.json")) {
                var lang = Objects.requireNonNull(resource.getFilename())
                        .replace(".json", "");
                var translations = objectMapper.readValue(
                        resource.getInputStream(),
                        new TypeReference<Map<String, String>>() {
                        });

                result.put(lang, translations);
            }

            return result;
        } catch (Exception e) {
            log.error("Error during loading translation from classpath", e);
        }

        return result;
    }
}
