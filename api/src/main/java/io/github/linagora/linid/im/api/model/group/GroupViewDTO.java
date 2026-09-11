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
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing an enriched group read from the {@code groups_view}.
 *
 * <p>The {@code createdBy} and {@code updatedBy} fields hold the full name of the referenced account
 * rather than its identifier.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Group view data transfer object")
public class GroupViewDTO {

    /**
     * Unique identifier of the group.
     */
    @Schema(description = "Unique identifier of the group", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    /**
     * Functional unique identifier of the group.
     */
    @Schema(description = "Functional unique identifier of the group", example = "developers")
    private String code;

    /**
     * Human-readable label of the group.
     */
    @Schema(description = "Human-readable label of the group", example = "Developers")
    private String label;

    /**
     * Identifier of the parent group.
     */
    @Schema(description = "Identifier of the parent group", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID parentId;

    /**
     * Label of the parent group.
     */
    @Schema(description = "Label of the parent group", example = "Engineering")
    private String parentLabel;

    /**
     * Free-text description of the group.
     */
    @Schema(description = "Free-text description of the group", example = "Software development team")
    private String description;

    /**
     * Email address of the group.
     */
    @Schema(description = "Email address of the group", example = "developers@example.com")
    private String email;

    /**
     * Identifier of the organizational unit associated with the group.
     */
    @Schema(description = "Identifier of the organizational unit associated with the group",
        example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID organizationalUnitId;

    /**
     * Name of the organizational unit associated with the group.
     */
    @Schema(description = "Name of the organizational unit associated with the group", example = "Engineering")
    private String organizationalUnitName;

    /**
     * Identifier of the application associated with the group.
     */
    @Schema(description = "Identifier of the application associated with the group",
        example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID applicationId;

    /**
     * Name of the application associated with the group.
     */
    @Schema(description = "Name of the application associated with the group", example = "My Application")
    private String applicationName;

    /**
     * Additional deployment-specific attributes stored as JSON.
     */
    @Schema(description = "Additional deployment-specific attributes stored as JSON")
    private Map<String, Object> extraParameters;

    /**
     * Full name of the creator of this record.
     */
    @Schema(description = "Full name of the record creator", example = "John Doe")
    private String createdBy;

    /**
     * Full name of the last updater of this record.
     */
    @Schema(description = "Full name of the record last updater", example = "John Doe")
    private String updatedBy;

    /**
     * Timestamp when the record was created.
     */
    @Schema(description = "Record creation date")
    private OffsetDateTime insertDate;

    /**
     * Timestamp when the record was last updated.
     */
    @Schema(description = "Record last update date")
    private OffsetDateTime updateDate;
}
