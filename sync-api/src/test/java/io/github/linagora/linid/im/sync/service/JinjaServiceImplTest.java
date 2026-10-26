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

package io.github.linagora.linid.im.sync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: JinjaServiceImpl")
class JinjaServiceImplTest {

    private final JinjaService service = new JinjaServiceImpl();

    @Test
    @DisplayName("render should substitute the variables of the context")
    void testRender_variable() {
        var result = service.render("{{ accounts.external_id }}", Map.of("accounts", Map.of("external_id", "jdoe")));

        assertEquals("jdoe", result);
    }

    @Test
    @DisplayName("render should insert a string with tojson as an escaped JSON string")
    void testRender_tojsonString() {
        var result = service.render("{{ name | tojson }}", Map.of("name", "R\"&D\\"));

        assertEquals("\"R\\\"&D\\\\\"", result);
    }

    @Test
    @DisplayName("render should insert a list with tojson as a JSON array")
    void testRender_tojsonList() {
        var result = service.render("{{ groups | tojson }}", Map.of("groups", List.of("DEV", "RND")));

        assertEquals("[\"DEV\",\"RND\"]", result);
    }

    @Test
    @DisplayName("render should insert an absent variable with tojson as null")
    void testRender_tojsonAbsent() {
        var result = service.render("{{ accounts.email | tojson }}", Map.of());

        assertEquals("null", result);
    }
}
