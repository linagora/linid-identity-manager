/*
 * Copyright (C) 2026 Linagora
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

import type { Page, Pagination } from '@linagora/linid-im-front-corelib';
import { api } from 'boot/axios';
import type {
  AccountDTO,
  AccountQueryFilterDTO,
  AccountRecord,
  AccountStatusRecord,
} from 'src/types/accounts';

/**
 * Retrieves a single account by its identifier from the backend.
 * @param id - The unique identifier of the account.
 * @returns A promise resolving to the raw account DTO returned by the API.
 */
export async function getAccountById(id: string): Promise<AccountDTO> {
  return api
    .get<AccountDTO>(`/accounts/${id}`)
    .then((response) => response.data);
}

/**
 * Creates a new account on the backend.
 * @param payload - The account fields submitted by the user.
 * @returns A promise resolving to the raw DTO of the newly created account.
 */
export async function createAccount(
  payload: AccountRecord
): Promise<AccountDTO> {
  return api
    .post<AccountDTO>('/accounts', payload)
    .then((response) => response.data);
}

/**
 * Retrieves accounts list from the API.
 * @param filters Object containing the filter criteria for querying accounts.
 * @param pagination Object containing pagination parameters.
 * @returns Promise of paginated accounts.
 */
export async function getAccounts(
  filters: AccountQueryFilterDTO,
  pagination: Pagination
): Promise<Page<AccountDTO>> {
  return api
    .get<
      Page<AccountDTO>
    >(`/accounts`, { params: { ...filters, ...pagination } })
    .then(({ data }) => data);
}

/**
 * Updates the account status, for actions such as activation, suspension, or deactivation.
 * @param id - The unique identifier of the account to update.
 * @param accountStatus - The account status payload containing the updated status information.
 * @returns A promise resolving to the raw DTO of the updated account, reflecting its new status.
 */
export function updateStatus(
  id: string,
  accountStatus: AccountStatusRecord
): Promise<AccountDTO> {
  return api
    .put<AccountDTO>(`/accounts/${id}/status`, accountStatus)
    .then((response) => response.data);
}
