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
import io.github.linagora.linid.im.api.persistence.model.AccountView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between {@link Account} entity and {@link AccountDTO}.
 */
@Mapper(componentModel = "spring", uses = CommonMapper.class)
public interface AccountMapper {

    /**
     * Creates a new {@link Account} entity from an {@link AccountRecord} and the calling principal.
     *
     * <p>Only the fields present in the record ({@code externalId}, {@code lastname}, {@code
     * firstname}, {@code email}) and the principal's identifier ({@code createdBy}, {@code
     * updatedBy}) are mapped. Computed fields ({@code payload}, {@code checksum}) are left unset and
     * must be populated by the caller after this method returns. {@code validityPeriod} is
     * intentionally omitted — it is stored in the companion {@code account_status} row, not on the
     * account itself.
     *
     * @param record        the creation request record
     * @param userPrincipal the authenticated principal performing the creation
     * @return a partially populated {@link Account} entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "payload", ignore = true)
    @Mapping(target = "checksum", ignore = true)
    @Mapping(target = "insertDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @Mapping(target = "createdBy", source = "userPrincipal.id")
    @Mapping(target = "updatedBy", source = "userPrincipal.id")
    @Mapping(target = "email", source = "record.email")
    Account toAccount(AccountRecord record, UserPrincipal userPrincipal);

    /**
     * Converts an {@link Account} entity to an {@link AccountDTO}.
     *
     * @param account the account entity
     * @return the corresponding DTO
     */
    AccountDTO toDTO(Account account);

    /**
     * Converts an {@link AccountView} entity to an {@link AccountViewDTO}.
     *
     * @param accountView the account view entity
     * @return the corresponding DTO
     */
    AccountViewDTO toDTO(AccountView accountView);
}
