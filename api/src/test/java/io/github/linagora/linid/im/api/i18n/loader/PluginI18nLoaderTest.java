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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Test class: PluginI18nLoader")
@ExtendWith(MockitoExtension.class)
class PluginI18nLoaderTest {

  private PluginI18nLoader loader;

  @BeforeEach
  void setUp() {
    loader = new PluginI18nLoader();
    ReflectionTestUtils.setField(loader, "pluginDirectoryPath", "plugins");
  }

  @Test
  @DisplayName("should return true for supports('plugin')")
  void shouldSupportPlugin() {
    assertTrue(loader.supports("plugin"));
    assertFalse(loader.supports("other"));
  }

  @Test
  @DisplayName("parse() should return map when valid i18n JSON entries are present in jar")
  void parseShouldReturnTranslations() throws IOException {
    // Given
    JarEntry entry = new JarEntry("i18n/en.json");
    byte[] jsonData = "{\"hello\":\"Hello\"}".getBytes();

    JarFile mockJar = Mockito.mock(JarFile.class);
    Mockito.when(mockJar.stream()).thenReturn(Stream.of(entry));
    Mockito.when(mockJar.getInputStream(entry)).thenReturn(new ByteArrayInputStream(jsonData));

    // When
    Map<String, Map<String, String>> result = loader.parse(mockJar);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.containsKey("en"));
    assertEquals("Hello", result.get("en").get("hello"));
  }

  @Test
  @DisplayName("parse() should skip entry and not fail when an exception occurs while reading")
  void parseShouldSkipEntryOnReadException() throws IOException {
    JarEntry entry = new JarEntry("i18n/en.json");

    JarFile mockJar = Mockito.mock(JarFile.class);
    Mockito.when(mockJar.stream()).thenReturn(Stream.of(entry));
    Mockito.when(mockJar.getInputStream(entry)).thenThrow(new RuntimeException("Simulated error"));

    Map<String, Map<String, String>> result = loader.parse(mockJar);

    assertNotNull(result);
    assertTrue(result.isEmpty(), "Expected no translations due to exception during parsing");
  }

  @Test
  @DisplayName("parse() should skip non-i18n entries in jar")
  void parseShouldIgnoreNonI18nFiles() {
    JarEntry entry = new JarEntry("META-INF/manifest.mf");

    JarFile mockJar = Mockito.mock(JarFile.class);
    Mockito.when(mockJar.stream()).thenReturn(Stream.of(entry));

    Map<String, Map<String, String>> result = loader.parse(mockJar);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("load() should return empty map if plugin directory is empty or missing")
  void loadShouldReturnEmptyMapIfNoJarsFound() {
    File pluginDir = Mockito.mock(File.class);
    File[] empty = new File[0];

    try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
      filesMock.when(() -> Files.walk(Paths.get("plugins"), 1))
          .thenReturn(Stream.of(empty));

      Map<String, Map<String, String>> result = loader.load();

      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }

  @Test
  @DisplayName("load() should use pluginDirectoryPath to walk files")
  void loadShouldUseInjectedPluginDirectoryPath() {
    Path fakeJar = Paths.get("plugins", "plugin.jar");

    try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
      filesMock.when(() -> Files.walk(Paths.get("plugins"), 1)).thenReturn(Stream.of(fakeJar));
      filesMock.when(() -> Files.isRegularFile(fakeJar)).thenReturn(true);

      try (MockedConstruction<JarFile> jarFileMock = Mockito.mockConstruction(JarFile.class,
          (mock, context) -> Mockito.when(mock.stream()).thenReturn(Stream.empty()))) {

        Map<String, Map<String, String>> result = loader.load();

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Expected result to be empty since no translations were parsed");

        filesMock.verify(() -> Files.walk(Paths.get("plugins"), 1));
      }
    }
  }

  @Test
  @DisplayName("load() should handle exception while reading jar and continue")
  void loadShouldHandleJarFileException() {
    Path fakeJarPath = Paths.get("plugins/fake-plugin.jar");
    try (
        MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class);
        MockedConstruction<JarFile> jarFileConstruction = Mockito.mockConstruction(JarFile.class,
            (mock, context) -> {
              throw new IOException("Simulated exception on JarFile constructor");
            })) {
      filesMock.when(() -> Files.walk(Paths.get("plugins"), 1))
          .thenReturn(Stream.of(fakeJarPath));
      filesMock.when(() -> Files.isRegularFile(fakeJarPath)).thenReturn(true);

      Map<String, Map<String, String>> result = loader.load();

      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }
}
