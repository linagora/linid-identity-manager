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

package io.github.linagora.linid.im.api.i18n.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: I18nMergeCollector")
class I18nMergeCollectorTest {
    @Test
    @DisplayName("Should merge two maps with different languages")
    void shouldMergeDifferentLanguages() {
        Map<String, Map<String, String>> map1 = Map.of("en", Map.of("greeting", "Hello"));
        Map<String, Map<String, String>> map2 = Map.of("fr", Map.of("greeting", "Bonjour"));

        Map<String, Map<String, String>> result = Stream.of(map1, map2)
                .collect(I18nMergeCollector.toCollect());

        Map<String, Map<String, String>> expected = new HashMap<>();
        expected.put("en", Map.of("greeting", "Hello"));
        expected.put("fr", Map.of("greeting", "Bonjour"));

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should merge translations under the same language key")
    void shouldMergeSameLanguage() {
        Map<String, Map<String, String>> map1 = Map.of("en", Map.of("greeting", "Hello"));
        Map<String, Map<String, String>> map2 = Map.of("en", Map.of("farewell", "Bye"));

        Map<String, Map<String, String>> result = Stream.of(map1, map2)
                .collect(I18nMergeCollector.toCollect());

        Map<String, Map<String, String>> expected = new HashMap<>();
        Map<String, String> en = new HashMap<>();
        en.put("greeting", "Hello");
        en.put("farewell", "Bye");
        expected.put("en", en);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should overwrite duplicate keys in same language")
    void shouldOverwriteDuplicateKeys() {
        Map<String, Map<String, String>> map1 = Map.of("en", Map.of("greeting", "Hi"));
        Map<String, Map<String, String>> map2 = Map.of("en", Map.of("greeting", "Hello"));

        Map<String, Map<String, String>> result = Stream.of(map1, map2)
                .collect(I18nMergeCollector.toCollect());

        Map<String, Map<String, String>> expected = Map.of("en", Map.of("greeting", "Hello"));

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should return empty map when stream is empty")
    void shouldReturnEmptyMapOnEmptyStream() {
        Map<String, Map<String, String>> result = Stream.<Map<String, Map<String, String>>>empty()
                .collect(I18nMergeCollector.toCollect());

        assertEquals(Map.of(), result);
    }
}
