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

import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitRelationViewDTO;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
import io.hypersistence.utils.hibernate.type.range.Range;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents an organizational unit within the system.
 * <p>An organizational unit is a logical business entity used to structure
 * departments, divisions, teams, subsidiaries, or any hierarchical grouping
 * within an organization.
 */
@Entity
@Immutable
@Table(name = "organizational_units_view")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrganizationalUnitDistinctView extends AbstractViewEntity {

    /**
     * Unique identifier of the organizational unit, auto-generated as UUID.
     */
    @Id
    @Column(name = "oun_id")
    private UUID id;

    /**
     * Human-readable name of the organizational unit.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Type of the organizational unit.
     * <p>
     * This value categorizes the unit according to business rules,
     * such as {@code DEPARTMENT}, {@code DIVISION}, {@code TEAM},
     * or other domain-specific classifications.</p>
     */
    @Column(name = "type", nullable = false)
    private String type;

    /**
     * List of parent organizational units linked to this organizational unit.
     *
     * <p>This field represents the hierarchy path of the current organizational unit.
     * Each entry describes a direct parent organizational unit.</p>
     */
    @Type(JsonType.class)
    @Column(name = "parents", nullable = false, columnDefinition = "jsonb")
    private List<OrganizationalUnitRelationViewDTO> parents;

    /**
     * Comma-separated names of all parent organizational units associated with
     * this organizational unit.
     */
    @Column(name = "parent_names")
    private String parentNames;

    /**
     * Time range during which the organizational unit is suspended. {@code null} when no suspension
     * is configured.
     */
    @Type(PostgreSQLRangeType.class)
    @Column(name = "suspension_period", columnDefinition = "tstzrange")
    private Range<ZonedDateTime> suspensionPeriod;

    /**
     * High-level reason code explaining the suspension. {@code null} when not
     * provided.
     */
    @Column(name = "suspension_reason")
    private String suspensionReason;

    /**
     * More detailed classification of the suspension reason. {@code null} when not provided.
     */
    @Column(name = "suspension_subreason")
    private String suspensionSubreason;

    /**
     * Free-text comment providing additional context about the suspension. {@code null} when not
     * provided.
     */
    @Column(name = "suspension_comment")
    private String suspensionComment;

    /**
     * Free-text comment providing additional context about the reactivation. {@code null} when not
     * provided.
     */
    @Column(name = "reactivation_comment")
    private String reactivationComment;

    /**
     * Computed flag indicating whether the organizational unit is currently suspended.
     */
    @Column(name = "is_suspended", nullable = false)
    private boolean suspended;

    /**
     * Creates an organizational unit view with its audit information, identity information, parent relationships,
     * parent names, and suspension information.
     *
     * <p>This constructor is intentionally provided with all view properties as parameters to allow
     * {@code spring-query-filter} to instantiate the entity when creating filtered query projections.
     *
     * @param createdBy the identifier of the user or system that created the organizational unit.
     * @param updatedBy the identifier of the user or system that last updated the organizational unit.
     * @param insertDate the timestamp when the organizational unit was created.
     * @param updateDate the timestamp when the organizational unit was last updated.
     * @param id the unique identifier of the organizational unit.
     * @param name the human-readable name of the organizational unit.
     * @param type the type of the organizational unit.
     * @param parents the parent organizational unit relationships associated with the organizational unit.
     * @param parentNames the names of all parent organizational units associated with the organizational unit,
     *                    represented as a comma-separated list.
     * @param suspensionPeriod the time range during which the organizational unit is suspended.
     * @param suspensionReason the high-level reason code explaining the organizational unit suspension.
     * @param suspensionSubreason the detailed classification of the organizational unit suspension reason.
     * @param suspensionComment the free-text comment providing additional context about the organizational unit
     *                          suspension.
     * @param reactivationComment the free-text comment providing additional context about the organizational unit
     *                            reactivation.
     * @param suspended whether the organizational unit is currently suspended.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public OrganizationalUnitDistinctView(final String createdBy,
                                          final String updatedBy,
                                          final OffsetDateTime insertDate,
                                          final OffsetDateTime updateDate,
                                          final UUID id,
                                          final String name,
                                          final String type,
                                          final List<OrganizationalUnitRelationViewDTO> parents,
                                          final String parentNames,
                                          final Range<ZonedDateTime> suspensionPeriod,
                                          final String suspensionReason,
                                          final String suspensionSubreason,
                                          final String suspensionComment,
                                          final String reactivationComment,
                                          final boolean suspended) {
        super(createdBy, updatedBy, insertDate, updateDate);
        this.id = id;
        this.name = name;
        this.type = type;
        this.parents = parents;
        this.parentNames = parentNames;
        this.suspensionPeriod = suspensionPeriod;
        this.suspensionReason = suspensionReason;
        this.suspensionSubreason = suspensionSubreason;
        this.suspensionComment = suspensionComment;
        this.reactivationComment = reactivationComment;
        this.suspended = suspended;
    }
}
