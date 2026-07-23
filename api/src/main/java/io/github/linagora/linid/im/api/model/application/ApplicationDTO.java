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

package io.github.linagora.linid.im.api.model.application;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing an application in API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Application data transfer object")
public class ApplicationDTO {

    /**
     * Unique identifier of the application.
     */
    @Schema(description = "Unique identifier of the application", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    /**
     * Functional unique identifier of the application.
     */
    @Schema(description = "Functional unique identifier of the application", example = "my-app")
    private String code;

    /**
     * Human-readable name of the application.
     */
    @Schema(description = "Human-readable name of the application", example = "My Application")
    private String name;

    /**
     * Free-text description of the application.
     */
    @Schema(description = "Free-text description of the application", example = "Internal HR application")
    private String description;

    /**
     * Type of the application.
     */
    @Schema(description = "Type of the application", example = "OIDC")
    private String type;

    /**
     * Template used to generate the claims of the application.
     */
    @Schema(description = "Template used to generate the claims of the application")
    private String claimsTemplate;

    /**
     * Date and time when the application script was deployed on OPA.
     */
    @Schema(description = "Date and time when the application script was deployed on OPA",
        example = "2026-06-29T10:15:30+00:00")
    private OffsetDateTime deployedAt;

    /**
     * JSON configuration of the application.
     */
    @Schema(description = "JSON configuration of the application", example = "{ \"timeout\": 30 }")
    private String configuration;


    /**
     * Identifier of the creator of this record.
     */
    @Schema(description = "Creator of the record", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID createdBy;

    /**
     * Identifier of the last updater of this record.
     */
    @Schema(description = "Last updater of the record", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID updatedBy;

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
