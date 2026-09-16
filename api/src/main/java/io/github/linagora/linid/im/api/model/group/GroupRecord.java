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

package io.github.linagora.linid.im.api.model.group;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

/**
 * Request payload for creating or updating a group.
 *
 * @param code                 functional unique identifier of the group
 * @param name                human-readable name of the group
 * @param parentId             optional identifier of the parent group
 * @param description          optional free-text description of the group
 * @param email                optional email address of the group
 * @param organizationalUnitId optional identifier of the organizational unit associated with the group
 * @param applicationId        optional identifier of the application associated with the group
 * @param extraParameters      additional deployment-specific attributes stored as JSON
 */
@Schema(description = "Request payload for creating or updating a group")
public record GroupRecord(
    @NotBlank
    @Size(max = GroupRecord.CODE_MAX_LENGTH)
    @Pattern(regexp = "^[a-zA-Z-_0-9]+$")
    @Schema(description = "Functional unique identifier of the group", example = "developers")
    String code,

    @NotBlank
    @Size(max = GroupRecord.NAME_MAX_LENGTH)
    @Schema(description = "Human-readable name of the group", example = "Developers")
    String name,

    @Schema(description = "Identifier of the parent group", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID parentId,

    @Schema(description = "Free-text description of the group", example = "Software development team")
    String description,

    @Email
    @Size(max = GroupRecord.EMAIL_MAX_LENGTH)
    @Schema(description = "Email address of the group", example = "developers@example.com")
    String email,

    @Schema(description = "Identifier of the organizational unit associated with the group",
        example = "550e8400-e29b-41d4-a716-446655440000")
    UUID organizationalUnitId,

    @Schema(description = "Identifier of the application associated with the group",
        example = "550e8400-e29b-41d4-a716-446655440000")
    UUID applicationId,

    @NotNull @Schema(description = "Additional deployment-specific attributes stored as JSON")
    Map<String, Object> extraParameters
) {

    /**
     * Maximum length of the group code, matching the {@code code} column size.
     */
    public static final int CODE_MAX_LENGTH = 100;

    /**
     * Maximum length of the group name, matching the {@code name} column size.
     */
    public static final int NAME_MAX_LENGTH = 255;

    /**
     * Maximum length of the group email, matching the {@code email} column size.
     */
    public static final int EMAIL_MAX_LENGTH = 320;
}
