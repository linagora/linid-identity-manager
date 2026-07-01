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

package io.github.linagora.linid.im.api.persistence.model;

import io.github.zorin95670.predicate.FilterType;
import io.github.zorin95670.processor.annotation.QueryFilter;
import io.github.zorin95670.processor.annotation.QueryFilterField;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Immutable;

/**
 * Entity with enriched application rule information, mapped to the {@code application_rules_view} database view.
 *
 * <p>Provides {@code createdBy} and {@code updatedBy} resolved to the full name of the referenced account,
 * along with {@code insertDate} and {@code updateDate}, inherited from {@link AbstractViewEntity}.</p>
 */
@Entity
@Immutable
@Table(name = "application_rules_view")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@QueryFilter
public class ApplicationRuleView extends AbstractViewEntity {

    /**
     * Unique identifier of the application rule (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "aru_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Application rule unique identifier")
    private UUID id;

    /**
     * Identifier of the application the rule belongs to.
     */
    @Column(name = "app_id", nullable = false)
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Identifier of the application the rule belongs to")
    private UUID applicationId;

    /**
     * Functional identifier of the rule, unique within a given application.
     */
    @Column(name = "code", nullable = false)
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Functional identifier of the rule")
    private String code;

    /**
     * Optional free-text description of the rule.
     */
    @Column(name = "description")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Free-text description of the rule")
    private String description;

    /**
     * Execution priority of the rule. Lower values are executed first.
     */
    @Column(name = "priority", nullable = false)
    @FilterType(type = Integer.class)
    @QueryFilterField(type = Integer.class, description = "Execution priority of the rule; lower values first")
    private Integer priority;

    /**
     * OPA Rego policy script computing the access rights granted by the rule.
     */
    @Column(name = "script", nullable = false)
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "OPA Rego policy script of the rule")
    private String script;

    /**
     * SHA-256 checksum computed from the script.
     */
    @Column(name = "script_checksum", nullable = false)
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Checksum computed from the rule script")
    private String scriptChecksum;

    /**
     * Whether the rule is disabled.
     */
    @Column(name = "disabled", nullable = false)
    @FilterType(type = Boolean.class)
    @QueryFilterField(type = Boolean.class, description = "Whether the rule is disabled")
    private Boolean disabled;
}
