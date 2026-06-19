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
 * JPA entity representing a user preference in the system.
 *
 * <p>Maps to the {@code user_preferences} table and stores a single key/value
 * preference scoped to a user (identified by email). Inherits audit fields from
 * {@link AbstractEntity}.</p>
 */
@Entity
@Table(name = "user_preferences")
@DynamicInsert
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserPreference extends AbstractEntity {

    /**
     * Unique identifier of the preference, auto-generated as UUID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "usp_id")
    @FilterType(type = UUID.class)
    private UUID id;

    /**
     * Email address of the account owning this preference.
     *
     * <p>Resolved server-side from the authenticated security context (JWT); it is never
     * provided by the client request body.</p>
     */
    @Column(name = "email", nullable = false)
    @FilterType(type = String.class)
    private String email;

    /**
     * Preference key (e.g. {@code theme}, {@code language}).
     *
     * <p>Unique per user: the pair ({@code email}, {@code key}) is constrained to be unique,
     * so the same key may exist for different users but only once per user.</p>
     */
    @Column(name = "key", nullable = false)
    @FilterType(type = String.class)
    private String key;

    /**
     * Preference value stored as plain text.
     */
    @Column(name = "value", nullable = false)
    @FilterType(type = String.class)
    private String value;
}
