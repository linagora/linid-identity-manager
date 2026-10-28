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

package io.github.linagora.linid.im.sync.model.syncdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

@DisplayName("Test class: SourceProperties")
class SourcePropertiesTest {

    @Test
    @DisplayName("accountIdColumn should keep the configured column")
    void testAccountIdColumn() {
        assertEquals("owner_id", new SourceProperties("owner_id").accountIdColumn());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("accountIdColumn should be act_id when it is not set")
    void testAccountIdColumn_default(final String accountIdColumn) {
        assertEquals("act_id", new SourceProperties(accountIdColumn).accountIdColumn());
    }

    @Test
    @DisplayName("accountIdColumn should be act_id for a source bound without settings")
    void testAccountIdColumn_boundWithoutSettings() {
        // A source written as "accounts: {}" in YAML is bound from an empty value
        var binder = new Binder(new MapConfigurationPropertySource(Map.of(
            "sync-data.job.sources.accounts", "",
            "sync-data.job.sources.phones.account-id-column", "owner_id")));

        var properties = binder.bind("sync-data.job", SyncDataProperties.class).get();

        assertEquals(Map.of("accounts", new SourceProperties("act_id"), "phones", new SourceProperties("owner_id")),
            properties.sources());
    }
}
