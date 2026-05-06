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

package io.github.linagora.linid.im.api.model.account;

import io.github.linagora.linid.im.api.model.common.CommonMapper;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Account;
import io.github.linagora.linid.im.api.persistence.model.AccountStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper between {@link AccountStatus} entities and their API representations.
 *
 * <p>Uses {@link CommonMapper} to convert between persistence
 * {@link io.hypersistence.utils.hibernate.type.range.Range Range&lt;ZonedDateTime&gt;}
 * values and the API records / DTOs
 * ({@link io.github.linagora.linid.im.api.model.common.PeriodDTO},
 * {@link io.github.linagora.linid.im.api.model.common.PeriodRecord}).</p>
 */
@Mapper(componentModel = "spring", uses = CommonMapper.class)
public interface AccountStatusMapper {

    /**
     * Creates an initial {@link AccountStatus} for a newly created account.
     *
     * <p>The validity period end is intentionally left open-ended ({@code null}) — only the start
     * date from the creation request is persisted. The end date can later be set via
     * {@code PUT /accounts/{id}/status}.</p>
     *
     * @param accountRecord the creation request record providing the validity period
     * @param userPrincipal the authenticated principal performing the creation
     * @param account       the persisted account whose ID must be linked
     * @return a new {@link AccountStatus} entity ready to be persisted
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "insertDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @Mapping(target = "suspensionPeriod", ignore = true)
    @Mapping(target = "activationAt", ignore = true)
    @Mapping(target = "statusReason", ignore = true)
    @Mapping(target = "statusSubreason", ignore = true)
    @Mapping(target = "statusComment", ignore = true)
    @Mapping(target = "createdBy", source = "userPrincipal.id")
    @Mapping(target = "updatedBy", source = "userPrincipal.id")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "validityPeriod", source = "accountRecord.validityPeriod")
    AccountStatus toAccountStatus(AccountRecord accountRecord, UserPrincipal userPrincipal, Account account);

    /**
     * Applies a pass-through {@link AccountStatusRecord} on top of an existing {@link AccountStatus}.
     *
     * <p>Every field carried by the record (including {@code null} values) overwrites the
     * corresponding field on the target entity, matching the endpoint's documented
     * pass-through semantics.</p>
     *
     * @param entity the entity to update in place
     * @param record the request record providing the new values
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "insertDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    void update(@MappingTarget AccountStatus entity, AccountStatusRecord record);
}
