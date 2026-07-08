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
  AccountDeactivationRecord,
  AccountDTO,
  AccountQueryFilterDTO,
  AccountReactivationRecord,
  AccountSuspensionRecord,
  AccountValidityRecord,
} from 'src/types/accounts';

/**
 * Retrieves a single account by its identifier from the backend.
 *
 * @param id - The unique identifier of the account.
 * @returns A promise resolving to the raw account DTO returned by the API.
 */
export async function getAccountById(id: string): Promise<AccountDTO> {
  return api
    .get<AccountDTO>(`/accounts/${id}`)
    .then((response) => response.data);
}

/**
 * Retrieves accounts list from the API.
 *
 * @param filters Object containing the filter criteria for querying accounts.
 * @param pagination Object containing pagination parameters.
 * @returns Promise of paginated accounts.
 */
export async function getAccounts(
  filters: AccountQueryFilterDTO,
  pagination: Pagination
): Promise<Page<AccountDTO>> {
  return api
    .get<Page<AccountDTO>>(`/accounts`, {
      params: { ...filters, ...pagination },
    })
    .then(({ data }) => data);
}

/**
 * Suspends an account, immediately or as a scheduled suspension depending on the suspension period start carried by the
 * payload.
 *
 * @param id - The unique identifier of the account to suspend.
 * @param payload - The suspension period and reason fields.
 * @returns A promise resolving to the raw DTO of the updated account.
 */
export function suspendAccount(
  id: string,
  payload: AccountSuspensionRecord
): Promise<AccountDTO> {
  return api
    .put<AccountDTO>(`/accounts/${id}/status/suspend`, payload)
    .then((response) => response.data);
}

/**
 * Deactivates an account (sets its validity period end), immediately or as a scheduled deactivation depending on the
 * deactivation timestamp carried by the payload.
 *
 * @param id - The unique identifier of the account to deactivate.
 * @param payload - The deactivation timestamp and reason fields.
 * @returns A promise resolving to the raw DTO of the updated account.
 */
export function deactivateAccount(
  id: string,
  payload: AccountDeactivationRecord
): Promise<AccountDTO> {
  return api
    .put<AccountDTO>(`/accounts/${id}/status/deactivate`, payload)
    .then((response) => response.data);
}

/**
 * Reactivates an account (lifts its suspension).
 *
 * @param id - The unique identifier of the account to reactivate.
 * @param payload - The mandatory justification comment.
 * @returns A promise resolving to the raw DTO of the updated account.
 */
export function reactivateAccount(
  id: string,
  payload: AccountReactivationRecord
): Promise<AccountDTO> {
  return api
    .put<AccountDTO>(`/accounts/${id}/status/reactivate`, payload)
    .then((response) => response.data);
}

/**
 * Schedules an account's validity period start (administrative action, distinct from the activation timestamp set when
 * the user clicks the activation link).
 *
 * @param id - The unique identifier of the account.
 * @param payload - The validity period start.
 * @returns A promise resolving to the raw DTO of the updated account.
 */
export function setAccountValidity(
  id: string,
  payload: AccountValidityRecord
): Promise<AccountDTO> {
  return api
    .put<AccountDTO>(`/accounts/${id}/status/schedule-activation`, payload)
    .then((response) => response.data);
}
