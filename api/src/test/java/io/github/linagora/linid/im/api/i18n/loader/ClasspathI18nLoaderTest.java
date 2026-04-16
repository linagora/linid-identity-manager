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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Test class: ClasspathI18nLoader")
class ClasspathI18nLoaderTest {
    private final ClasspathI18nLoader loader = new ClasspathI18nLoader();
    private final PathMatchingResourcePatternResolver mockedResolver = Mockito
            .mock(PathMatchingResourcePatternResolver.class);

    @BeforeEach
    void injectMockedResolver() {
        Mockito.reset(mockedResolver);
        ReflectionTestUtils.setField(loader, "resolver", mockedResolver);
    }

    @Test
    @DisplayName("should return true for supports('internal')")
    void shouldSupportInternal() {
        assertTrue(loader.supports("internal"));
        assertFalse(loader.supports("other"));
    }

    @Test
    @DisplayName("should load mocked translation file from classpath")
    void shouldLoadMockedTranslationFromClasspath() throws Exception {
        String mockJson = "{\"hello\": \"Hello World\", \"bye\": \"Goodbye\"}";

        Resource mockResource = Mockito.mock(Resource.class);
        Mockito.when(mockResource.getFilename()).thenReturn("en.json");
        Mockito.when(mockResource.getInputStream()).thenReturn(
                new ByteArrayInputStream(mockJson.getBytes(StandardCharsets.UTF_8)));

        Mockito.when(mockedResolver.getResources("classpath*:i18n/*.json"))
                .thenReturn(new Resource[] { mockResource });

        Map<String, Map<String, String>> result = loader.load();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("en"));
        assertEquals(2, result.get("en").size());
        assertEquals("Hello World", result.get("en").get("hello"));
        assertEquals("Goodbye", result.get("en").get("bye"));
    }

    @Test
    @DisplayName("should return empty map if an exception occurs during resource loading")
    void shouldReturnEmptyMapOnException() throws Exception {
        Mockito.when(mockedResolver.getResources("classpath*:i18n/*.json"))
                .thenThrow(new RuntimeException("Simulated failure"));

        Map<String, Map<String, String>> result = loader.load();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
