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
import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
import io.hypersistence.utils.hibernate.type.range.Range;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.Type;

/**
 * JPA entity representing the suspension status lifecycle of an organizational unit.
 *
 * <p>Maps to the {@code organizational_unit_status} table. There is a one-to-one relationship with
 * {@link OrganizationalUnit} enforced by the unique index on {@code oun_id}. A status row is
 * automatically created by a database trigger whenever an organizational unit is inserted, so this
 * entity is only ever updated, never inserted, by the application layer. Inherits audit fields from
 * {@link AbstractEntity}.</p>
 */
@Entity
@Table(name = "organizational_unit_status")
@DynamicInsert
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrganizationalUnitStatus extends AbstractEntity {

    /**
     * Unique identifier of the organizational unit status record, auto-generated as UUID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ous_id")
    @FilterType(type = UUID.class)
    private UUID id;

    /**
     * Identifier of the owning organizational unit.
     */
    @Column(name = "oun_id", nullable = false)
    @FilterType(type = UUID.class)
    private UUID organizationalUnitId;

    /**
     * Time range during which the organizational unit is suspended. Stored as {@code TSTZRANGE}.
     * {@code null} when no suspension is configured.
     */
    @Type(PostgreSQLRangeType.class)
    @Column(name = "suspension_period", columnDefinition = "tstzrange")
    private Range<ZonedDateTime> suspensionPeriod;

    /**
     * High-level reason code explaining the current status. {@code null} when not provided.
     */
    @Column(name = "status_reason")
    @FilterType(type = String.class)
    private String statusReason;

    /**
     * More detailed classification of the status reason. {@code null} when not provided.
     */
    @Column(name = "status_subreason")
    @FilterType(type = String.class)
    private String statusSubreason;

    /**
     * Free-text comment providing additional context about the status change.
     * {@code null} when not provided.
     */
    @Column(name = "status_comment")
    @FilterType(type = String.class)
    private String statusComment;
}
