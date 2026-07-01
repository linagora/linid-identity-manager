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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for creating or updating an application rule.
 *
 * <p>The {@code disabled} state enables ({@code false}) or disables ({@code true}) the rule. On update it
 * is applied as provided; on creation it is ignored, as rules are always created disabled. The
 * {@code scriptChecksum} is computed from the {@code script} by the service and is therefore not part of
 * the request.</p>
 *
 * @param code        functional identifier of the rule, unique within the application
 * @param description optional free-text description of the rule
 * @param priority    execution priority of the rule; lower values are executed first
 * @param script      OPA Rego policy script computing the access rights granted by the rule
 * @param disabled    whether the rule is disabled; applied on update, ignored on creation
 */
@Schema(description = "Request payload for creating or updating an application rule")
public record ApplicationRuleRecord(
    @NotBlank @Schema(description = "Functional identifier of the rule", example = "RULE_2")
    String code,

    @Schema(description = "Free-text description of the rule", example = "Second rule")
    String description,

    @NotNull @Schema(description = "Execution priority of the rule; lower values are executed first",
        example = "2")
    Integer priority,

    @NotBlank @Schema(description = "OPA Rego policy script computing the access rights granted by the rule",
        example = "return false;")
    String script,

    @NotNull @Schema(description = "Whether the rule is disabled; applied on update, ignored on creation",
        example = "false")
    Boolean disabled
) {
}
