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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Test class: ExternalPathI18nLoader")
@ExtendWith(MockitoExtension.class)
class ExternalPathI18nLoaderTest {

    private ExternalPathI18nLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ExternalPathI18nLoader();
        ReflectionTestUtils.setField(loader, "i18nExternalPath", "some/fake/path");
    }

    @Test
    @DisplayName("should return true for supports('external')")
    void shouldSupportExternal() {
        assertTrue(loader.supports("external"));
        assertFalse(loader.supports("other"));
    }

    @Test
    @DisplayName("test load: should return translations when JSON files are present")
    void loadShouldReturnTranslationsWhenFilesArePresent() {
        Path path1 = Paths.get("some/fake/path/en.json");
        Path path2 = Paths.get("some/fake/path/fr.json");

        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.walk(Mockito.any(Path.class))).thenReturn(Stream.of(path1, path2));
            filesMock.when(() -> Files.isRegularFile(path1)).thenReturn(true);
            filesMock.when(() -> Files.isRegularFile(path2)).thenReturn(true);

            filesMock.when(() -> Files.newInputStream(path1))
                    .thenReturn(new ByteArrayInputStream("{\"hello\":\"Hello\"}".getBytes()));
            filesMock.when(() -> Files.newInputStream(path2))
                    .thenReturn(new ByteArrayInputStream("{\"hello\":\"Bonjour\"}".getBytes()));

            var result = loader.load();

            assertEquals(2, result.size());
            assertEquals("Hello", result.get("en").get("hello"));
            assertEquals("Bonjour", result.get("fr").get("hello"));
        }
    }

    @Test
    @DisplayName("load() should handle IOException and return empty result")
    void loadShouldHandleIOExceptionAndReturnEmptyResult() {
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.walk(Mockito.any(Path.class)))
                    .thenThrow(new IOException("Simulated IO error"));

            Map<String, Map<String, String>> result = loader.load();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @DisplayName("test parse: should return null when IOException occurs")
    void parseShouldReturnNullWhenIOExceptionOccurs() {
        Path badPath = Paths.get("bad/path/de.json");

        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(badPath))
                    .thenThrow(new IOException("fail"));

            assertNull(loader.parse(badPath));
        }
    }

    @Test
    @DisplayName("test parse: should return map entry when JSON is valid")
    void parseShouldReturnEntryWhenValidJson() {
        Path path = Paths.get("some/fake/path/es.json");
        String json = "{\"welcome\":\"Bienvenido\"}";

        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.newInputStream(path)).thenReturn(toInputStream(json));

            Map.Entry<String, Map<String, String>> entry = loader.parse(path);

            assertNotNull(entry);
            assertEquals("es", entry.getKey());
            assertEquals(Map.of("welcome", "Bienvenido"), entry.getValue());
        }
    }

    private InputStream toInputStream(String json) {
        return new ByteArrayInputStream(json.getBytes());
    }
}
