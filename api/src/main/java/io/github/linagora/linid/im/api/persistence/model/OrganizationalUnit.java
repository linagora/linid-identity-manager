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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;

import java.util.UUID;

/**
 * Represents an organizational unit within the system.
 * <p>An organizational unit is a logical business entity used to structure
 * departments, divisions, teams, subsidiaries, or any hierarchical grouping
 * within an organization.
 *
 * <p>This entity is mapped to the {@code organizational_units} database table.
 * UUID identifiers are automatically generated and dynamic inserts are enabled
 * to allow database-level default values when applicable.
 */
@Entity
@Table(name = "organizational_units")
@DynamicInsert
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@QueryFilter
public class OrganizationalUnit extends AbstractEntity {

    /**
     * Unique identifier of the organizational unit, auto-generated as UUID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "oun_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Organizational unit unique identifier.")
    private UUID id;

    /**
     * Human-readable name of the organizational unit.
     */
    @Column(name = "name", nullable = false)
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Organizational unit name.")
    private String name;

    /**
     * Type of the organizational unit.
     * <p>
     * This value categorizes the unit according to business rules,
     * such as {@code DEPARTMENT}, {@code DIVISION}, {@code TEAM},
     * or other domain-specific classifications.
     * </p>
     */
    @Column(name = "type", nullable = false)
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Organizational unit type.")
    private String type;
}
