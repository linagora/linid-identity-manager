/*
 * Copyright (C) 2026 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
 * which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
 * of the interface window, the display of the "You are using the Open Source and free version of LinID™, powered by
 * Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!" infobox and in the e-mails
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

import dayjs from 'dayjs';

/**
 * Creates a toDate function with the specified date format. Useful for creating consistent date formatting in test
 * mocks.
 *
 * @param dateFormat - The date format string (e.g., 'YYYY-MM-DD HH:mm', 'DD/MM/YYYY').
 * @returns A toDate function that formats dates consistently.
 */
export const mockToDate = (dateFormat: string) => (value: any) =>
  value ? dayjs(value).format(dateFormat) : '';

/**
 * Creates a toDateISO function. This is the same implementation for all test mocks.
 *
 * @returns A toDateISO function that converts dates to ISO 8601 format.
 */
export const mockToDateISO = () => (value: any) => {
  if (!value) {
    return '';
  }
  if (
    typeof value === 'string' &&
    value.match(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/)
  ) {
    return value;
  }
  return dayjs(value).format('YYYY-MM-DD[T]HH:mm:ss.000[Z]');
};

/**
 * Creates a toDayJs function. This is the same implementation for all test mocks.
 *
 * @returns A toDayJs function that converts dates to dayjs objects.
 */
export const mockToDayJs = () => (value: any) => (value ? dayjs(value) : null);
