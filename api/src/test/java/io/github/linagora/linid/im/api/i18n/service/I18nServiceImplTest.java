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

package io.github.linagora.linid.im.api.i18n.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.linagora.linid.im.api.i18n.loader.I18nSourceLoader;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: I18nServiceImpl")
class I18nServiceImplTest {

    private I18nServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new I18nServiceImpl(
                List.of("plugin", "external", "internal"),
                List.of(
                        new I18nSourceLoaderTest("internal"),
                        new I18nSourceLoaderTest("external"),
                        new I18nSourceLoaderTest("plugin")));
    }

    @Test
    @DisplayName("test getLanguages: should return all languages")
    void testGetLanguages() {
        service.run();
        assertEquals(List.of("en", "fr", "es"), service.getLanguages());
    }

    @Test
    @DisplayName("test getTranslations: should return all translations on valid language")
    void testGetTranslationsValidLanguage() {
        service.run();
        assertEquals(
                Map.of(
                        "type", "internal",
                        "external", "test-fr",
                        "internal", "test-fr",
                        "plugin", "test-fr",
                        "context", "{value} fr",
                        "test", "{I18N_LANGUAGE} - {I18N_KEY}"),
                service.getTranslations("fr"));
    }

    @Test
    @DisplayName("test getTranslations: should return empty translations on invalid language")
    void testGetTranslationsInvalidLanguage() {
        service.run();

        ApiException exception = assertThrows(ApiException.class, () -> {
            service.getTranslations("test");
        });

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.router.unknown.route", exception.getError().key());
        assertEquals("/i18n/test.json", exception.getError().context().getOrDefault("route", ""));
    }

    @Test
    @DisplayName("test translate: should translate in en by default")
    void testTranslateDefault() {
        service.run();
        assertEquals("test-en", service.translate(I18nMessage.of("plugin")));
    }

    @Test
    @DisplayName("test translate: should translate in wanted language")
    void testTranslate() {
        service.run();
        assertEquals("test-fr", service.translate("fr", I18nMessage.of("plugin")));
    }

    @Test
    @DisplayName("test translate: should return error message on invalid key")
    void testTranslateInvalidKey() {
        service.run();
        assertEquals("Unknown key \"bad\" for language \"fr\".", service.translate("fr", I18nMessage.of("bad")));
    }

    @Test
    @DisplayName("test translate: should use context")
    void testTranslateWithContext() {
        service.run();
        assertEquals("fr - test", service.translate("fr", I18nMessage.of("test")));
        assertEquals("yolo fr", service.translate("fr", I18nMessage.of("context", Map.of("value", "yolo"))));
    }

    @Test
    @DisplayName("test run: should organise translation from order")
    void testRun() {
        service = new I18nServiceImpl(
                List.of("external", "internal", "plugin"),
                List.of(
                        new I18nSourceLoaderTest("internal"),
                        new I18nSourceLoaderTest("external"),
                        new I18nSourceLoaderTest("plugin")));

        service.run();
        assertEquals(
                Map.of(
                        "type", "plugin",
                        "external", "test-fr",
                        "internal", "test-fr",
                        "plugin", "test-fr",
                        "context", "{value} fr",
                        "test", "{I18N_LANGUAGE} - {I18N_KEY}"),
                service.getTranslations("fr"));
    }

    @Data
    @AllArgsConstructor
    public class I18nSourceLoaderTest implements I18nSourceLoader {

        private String supportedType;

        @Override
        public boolean supports(@NonNull String type) {
            return supportedType.equals(type);
        }

        @Override
        public Map<String, Map<String, String>> load() {
            if ("plugin".equals(supportedType)) {
                return Map.of(
                        "fr",
                        Map.of("type", "plugin", "plugin", "test-fr"),
                        "en",
                        Map.of("type", "plugin", "plugin", "test-en"));
            }
            if ("external".equals(supportedType)) {
                return Map.of(
                        "fr",
                        Map.of("type", "external", "external", "test-fr", "context", "{value} fr"),
                        "en",
                        Map.of("type", "external", "external", "test-en", "context", "{value} en"));
            }
            return Map.of(
                    "fr",
                    Map.of("type", "internal", "internal", "test-fr", "test", "{I18N_LANGUAGE} - {I18N_KEY}"),
                    "en",
                    Map.of("type", "internal", "internal", "test-en", "test", "{I18N_LANGUAGE} - {I18N_KEY}"),
                    "es",
                    Map.of("type", "internal", "internal", "test-es", "test", "{I18N_LANGUAGE} - {I18N_KEY}"));
        }
    }
}
