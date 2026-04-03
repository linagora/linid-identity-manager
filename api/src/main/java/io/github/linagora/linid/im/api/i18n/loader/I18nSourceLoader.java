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

import java.util.Map;
import org.springframework.lang.NonNull;

/**
 * Strategy interface for loading i18n (internationalization) resources.
 *
 * <p>
 * Implementations are expected to load translations from a specific source
 * (e.g., classpath, external path, plugins), and
 * optionally declare if they support a specific type (used to determine load
 * order or filtering).
 */
public interface I18nSourceLoader {
    /**
     * Indicates whether this loader supports the given type.
     *
     * <p>
     * This is typically used to match configuration values (e.g., "classpath",
     * "external", "plugin").
     *
     * @param type the source type to check support for (non-null)
     * @return {@code true} if this loader supports the given type; {@code false}
     *         otherwise
     */
    boolean supports(@NonNull String type);

    /**
     * Loads i18n translation files and returns them as a map.
     *
     * <p>
     * The returned structure is a nested map where the outer key is the language
     * code (e.g., "en", "fr"), and the inner map
     * contains key/value pairs for translation entries.
     *
     * @return a map of language codes to their corresponding translation entries
     */
    Map<String, Map<String, String>> load();
}
