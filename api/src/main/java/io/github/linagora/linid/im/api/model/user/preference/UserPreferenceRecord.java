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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for the {@code POST /user-preferences} endpoint.
 *
 * <p>The same payload is used both to create a new user preference and to update an existing one
 * (upsert): if a user preference with the given key already exists for the authenticated user, its
 * value is overwritten; otherwise a new user preference is created.</p>
 *
 * <p>The owning user is resolved server-side from the authenticated security context (JWT) and is
 * never part of this payload, which is why no user/email field appears here.</p>
 *
 * @param key   the user preference key. Must match {@code ^[a-z]+([-_.][a-z]+)*$}: letters (upper or lower case),
 *              with {@code -}, {@code _} or {@code .} allowed solely as separators between
 *              letters (no leading, trailing or doubled separator). Unique per user.
 * @param value the user preference value, stored as free-form text (no format constraint beyond being
 *              non-blank)
 */
@Schema(description = "Request payload for creating or updating a user preference")
public record UserPreferenceRecord(
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z]+([-_.][a-zA-Z]+)*$")
        @Schema(description = "User preference key ", example = "theme")
        String key,

        @NotBlank
        @Schema(description = "User preference value", example = "dark")
        String value
) {
}
