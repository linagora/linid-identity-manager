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

package io.github.linagora.linid.im.api.service;

import io.github.linagora.linid.im.api.persistence.model.Account;
import io.github.linagora.linid.im.api.persistence.model.AccountQueryFilterDto;
import io.github.linagora.linid.im.api.model.account.AccountRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for account management operations.
 */
public interface AccountService {

  /**
   * Creates a new account from the given request.
   *
   * @param userPrincipal the authenticated user
   * @param account       the account creation record
   * @return the created account entity
   */
  Account create(UserPrincipal userPrincipal, AccountRecord account);

  /**
   * Retrieves a paginated list of accounts, optionally filtered.
   *
   * @param userPrincipal the authenticated user
   * @param filters       generated filter DTO from query parameters
   * @param pageable      pagination information
   * @return a page of account entities
   */
  Page<Account> findAll(UserPrincipal userPrincipal, AccountQueryFilterDto filters, Pageable pageable);

  /**
   * Retrieves an account by its unique identifier.
   *
   * @param userPrincipal the authenticated user
   * @param id            the account UUID
   * @return the account entity
   * @throws io.github.linagora.linid.im.corelib.exception.ApiException if not found
   */
  Account findById(UserPrincipal userPrincipal, UUID id);

  /**
   * Deletes an account by its unique identifier.
   *
   * @param userPrincipal the authenticated user
   * @param id            the account UUID
   * @throws io.github.linagora.linid.im.corelib.exception.ApiException if not found
   */
  void deleteById(UserPrincipal userPrincipal, UUID id);
}
