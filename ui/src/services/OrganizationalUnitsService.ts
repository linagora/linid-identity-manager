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
  OrganizationalUnitDTO,
  OrganizationalUnitFilterDTO,
} from 'src/types/organizationalUnits';

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
 * Fetches all organizational units from the API by iterating pages of 50
 * until the last page is reached.
 * @returns Promise resolving to the full list of organizational units.
 */
export async function getAllOrganizationalUnit(): Promise<OrganizationalUnitDTO[]> {
  const PAGE_SIZE = 5;
  const result: OrganizationalUnitDTO[] = [];
  let page = 0;
  let isLast = false;

  while (!isLast) {
    const response = await getOrganizationalUnits(
      { name: null },
      { page, size: PAGE_SIZE }
    );
    result.push(...response.content);
    isLast = response.last;
    page++;
  }

  return result;
}
