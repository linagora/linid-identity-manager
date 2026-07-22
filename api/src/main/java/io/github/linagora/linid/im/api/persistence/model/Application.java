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

import io.github.linagora.linid.im.api.model.application.ApplicationRoleDTO;
import io.github.zorin95670.predicate.FilterType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity representing an application in the system.
 *
 * <p>Maps to the {@code applications} table and includes identification fields, a claims template, an
 * optional provisioning script with its checksum, an optional configuration, and a list of roles.
 * Inherits audit fields from {@link AbstractEntity}.</p>
 */
@Entity
@Table(name = "applications")
@DynamicInsert
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Application extends AbstractEntity {

    /**
     * Unique identifier of the application, auto-generated as UUID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "app_id")
    @FilterType(type = UUID.class)
    private UUID id;

    /**
     * Functional unique identifier of the application.
     */
    @Column(name = "code", nullable = false)
    @FilterType(type = String.class)
    private String code;

    /**
     * Human-readable name of the application.
     */
    @Column(name = "name", nullable = false)
    @FilterType(type = String.class)
    private String name;

    /**
     * Optional free-text description of the application.
     */
    @Column(name = "description")
    @FilterType(type = String.class)
    private String description;

    /**
     * Type of the application.
     */
    @Column(name = "type", nullable = false)
    @FilterType(type = String.class)
    private String type;

    /**
     * Template used to generate the claims exposed to the application.
     */
    @Column(name = "claims_template", nullable = false)
    @FilterType(type = String.class)
    private String claimsTemplate;

    /**
     * Optional provisioning or transformation script associated with the application.
     */
    @Column(name = "script")
    @FilterType(type = String.class)
    private String script;

    /**
     * SHA-256 checksum computed from the script for change detection. {@code null} when no script is defined.
     */
    @Column(name = "script_checksum")
    @FilterType(type = String.class)
    private String scriptChecksum;

    /**
     * Optional date and time when the application script was deployed on OPA. {@code null} when the
     * application has not yet been deployed or requires redeployment.
     */
    @Column(name = "deployed_at")
    @FilterType(type = Date.class)
    private OffsetDateTime deployedAt;

    /**
     * JSONB configuration of the application.
     */
    @Column(name = "configuration", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @FilterType(type = String.class)
    private String configuration;

    /**
     * List of application roles, stored as a JSONB array of role objects (name and optional description).
     */
    @Type(JsonType.class)
    @Column(name = "roles", columnDefinition = "jsonb")
    private List<ApplicationRoleDTO> roles;
}
