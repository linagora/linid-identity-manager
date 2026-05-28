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
import { appConfig } from 'boot/config';
import type { AccountDTO, AccountQueryFilterDTO } from 'src/types/accounts';
import type {
  OrganizationalUnitDTO,
  OrganizationalUnitFilterDTO,
  OrganizationalUnitRecord,
  OrganizationalUnitStatusRecord,
} from 'src/types/organizationalUnits';

/**
 * Retrieves a single organizational unit by its identifier from the backend.
 * @param id - The unique identifier of the organizational unit.
 * @returns A promise resolving to the raw OU DTO returned by the API.
 */
export async function getOrganizationalUnitById(
  id: string
): Promise<OrganizationalUnitDTO> {
  return api
    .get<OrganizationalUnitDTO>(`/organizational-units/${id}`)
    .then((response) => response.data);
}

/**
 * Creates a new organizational unit on the backend.
 * @param payload - The OU fields submitted by the user, including the parent identifier provided by the navigation context.
 * @returns A promise resolving to the raw DTO of the newly created OU.
 */
export async function createOrganizationalUnit(
  payload: OrganizationalUnitRecord
): Promise<OrganizationalUnitDTO> {
  return api
    .post<OrganizationalUnitDTO>('/organizational-units', payload)
    .then((response) => response.data);
}

/**
 * Retrieves Organizational Units list from the API.
 * @param filters Object containing the filter criteria for querying Organizational Units.
 * @param pagination Object containing pagination parameters.
 * @returns Promise of paginated Organizational Units.
 */
export async function getOrganizationalUnits(
  filters: OrganizationalUnitFilterDTO,
  pagination: Pagination
): Promise<Page<OrganizationalUnitDTO>> {
  return api
    .get<
      Page<OrganizationalUnitDTO>
    >(`/organizational-units`, { params: { ...filters, ...pagination } })
    .then(({ data }) => data);
}

/**
 * Fetches all organizational units from the API.
 * @returns Promise resolving to the full list of organizational units.
 */
export async function getAllOrganizationalUnit(): Promise<
  OrganizationalUnitDTO[]
> {
  const result: OrganizationalUnitDTO[] = [];
  let page = 0;
  let isLast = false;

  while (!isLast) {
    const response = await getOrganizationalUnits(
      { name: null },
      { page, size: appConfig.organizationalUnitQuerySize }
    );
    result.push(...response.content);
    isLast = response.last;
    page++;
  }

  return result;
}

/**
 * Retrieves accounts list from the API by organizational unit id.
 * @param id The unique identifier of the organizational unit for which to retrieve accounts.
 * @param filters Object containing the filter criteria for querying accounts.
 * @param pagination Object containing pagination parameters.
 * @returns Promise of paginated accounts.
 */
export async function getAccountsByOrganizationalUnitId(
  id: string,
  filters: AccountQueryFilterDTO,
  pagination: Pagination
): Promise<Page<AccountDTO>> {
  return api
    .get<
      Page<AccountDTO>
    >(`/organizational-units/${id}/accounts`, { params: { ...filters, ...pagination } })
    .then(({ data }) => data);
}

/**
 * Updates the suspension status of an organizational unit.
 * @param id - The unique identifier of the organizational unit.
 * @param payload - The suspension period and optional reason fields.
 * @returns A promise resolving to the raw DTO of the updated OU.
 */
export async function updateOrganizationalUnitStatus(
  id: string,
  payload: OrganizationalUnitStatusRecord
): Promise<OrganizationalUnitDTO> {
  return api
    .put<OrganizationalUnitDTO>(`/organizational-units/${id}/status`, payload)
    .then((response) => response.data);
}
