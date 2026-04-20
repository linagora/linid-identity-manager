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
 * Entity with enriched account information, mapped to the {@code accounts_view} database view.
 *
 * <p>Provides {@code createdBy}, {@code updatedBy}, {@code insertDate}, and {@code updateDate}
 * columns that are automatically managed by the service layer and database triggers.
 */
@Entity
@Table(name = "accounts_view")
@Data
@Immutable
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@QueryFilter
public class AccountView extends AbstractViewEntity {

    /**
     * Unique identifier of the account (UUID).
     */
    @Id
    @Column(name = "act_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Account unique identifier")
    private UUID id;

    /**
     * External identifier (e.g. OIDC sub or external system ID).
     */
    @Column(name = "external_id", nullable = false)
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "External identifier (e.g. OIDC sub)")
    private String externalId;

    /**
     * Last name of the account holder.
     */
    @Column(name = "lastname", nullable = false)
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Last name of the account holder")
    private String lastname;

    /**
     * First name of the account holder.
     */
    @Column(name = "firstname", nullable = false)
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "First name of the account holder")
    private String firstname;

    /**
     * Email address associated with the account.
     */
    @Column(name = "email", nullable = false)
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Email address of the account")
    private String email;
}
