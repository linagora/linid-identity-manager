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

package io.github.linagora.linid.im.api.model.application.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing an application rule in API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Application rule data transfer object")
public class ApplicationRuleDTO {

    /**
     * Unique identifier of the application rule.
     */
    @Schema(description = "Unique identifier of the application rule", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    /**
     * Identifier of the application the rule belongs to.
     */
    @Schema(description = "Identifier of the application the rule belongs to",
        example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID applicationId;

    /**
     * Functional identifier of the rule.
     */
    @Schema(description = "Functional identifier of the rule", example = "RULE_2")
    private String code;

    /**
     * Free-text description of the rule.
     */
    @Schema(description = "Free-text description of the rule", example = "Second rule")
    private String description;

    /**
     * Execution priority of the rule; lower values are executed first.
     */
    @Schema(description = "Execution priority of the rule; lower values are executed first", example = "2")
    private Integer priority;

    /**
     * OPA Rego policy script computing the access rights granted by the rule.
     */
    @Schema(description = "OPA Rego policy script computing the access rights granted by the rule",
        example = "return false;")
    private String script;

    /**
     * Whether the rule is disabled.
     */
    @Schema(description = "Whether the rule is disabled", example = "true")
    private Boolean disabled;

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
