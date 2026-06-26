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

package io.github.linagora.linid.im.api.model.user.preference;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Data Transfer Object representing a single user preference in API responses.
 *
 * <p>Returned by {@code POST /user-preferences} to confirm the key/value that was created or updated.
 * Unlike most DTOs, it deliberately exposes neither identifier nor audit fields: a user preference is a
 * lightweight key/value pair and the client only needs the pair back.</p>
 *
 * <p>Note that {@code GET /user-preferences} does not use this DTO: it returns the user's preferences
 * aggregated as a {@code {key: value}} map rather than a list of objects.</p>
 */
@Data
@Schema(description = "Data Transfer Object representing a user preference")
public class UserPreferenceDTO {

    /**
     * Identifier of the user preference, unique per user. Echoes back the key that was sent in the
     * {@code POST /user-preferences} request body.
     */
    @Schema(description = "User preference key", example = "theme")
    private String key;

    /**
     * Current value associated with the key after the create or update was applied. Free-form text,
     * with no format constraint.
     */
    @Schema(description = "User preference value", example = "dark")
    private String value;
}
