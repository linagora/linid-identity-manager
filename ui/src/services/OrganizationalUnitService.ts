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

import { api } from 'boot/axios';
import type {
  OrganizationalUnitDTO,
  OrganizationalUnitReactivationRecord,
  OrganizationalUnitRecord,
  OrganizationalUnitSuspensionRecord,
} from 'src/types/organizationalUnits';

/**
 * Retrieves a single organizational unit by its identifier from the backend.
 *
 * @param id - The unique identifier of the organizational unit.
 * @param signal - Optional abort signal used to cancel a stale request.
 * @returns A promise resolving to the raw OU DTO returned by the API.
 */
export async function getOrganizationalUnitById(
  id: string,
  signal?: AbortSignal
): Promise<OrganizationalUnitDTO> {
  return api
    .get<OrganizationalUnitDTO>(`/organizational-units/${id}`, { signal })
    .then((response) => response.data);
}

/**
 * Creates a new organizational unit on the backend.
 *
 * @param payload - The OU fields submitted by the user, including the parent identifier provided by the navigation
 *   context.
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
 * Suspends an organizational unit, immediately or as a scheduled suspension depending on the suspension period start
 * carried by the payload.
 *
 * @param id - The unique identifier of the organizational unit.
 * @param payload - The suspension period and reason fields.
 * @returns A promise resolving to the raw DTO of the updated OU.
 */
export async function suspendOrganizationalUnit(
  id: string,
  payload: OrganizationalUnitSuspensionRecord
): Promise<OrganizationalUnitDTO> {
  return api
    .put<OrganizationalUnitDTO>(
      `/organizational-units/${id}/status/suspend`,
      payload
    )
    .then((response) => response.data);
}

/**
 * Reactivates an organizational unit (lifts its suspension).
 *
 * @param id - The unique identifier of the organizational unit.
 * @param payload - The mandatory justification comment.
 * @returns A promise resolving to the raw DTO of the updated OU.
 */
export async function reactivateOrganizationalUnit(
  id: string,
  payload: OrganizationalUnitReactivationRecord
): Promise<OrganizationalUnitDTO> {
  return api
    .put<OrganizationalUnitDTO>(
      `/organizational-units/${id}/status/reactivate`,
      payload
    )
    .then((response) => response.data);
}
