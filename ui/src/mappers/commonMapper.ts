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

import dayjs from 'dayjs';
import { useI18n } from 'vue-i18n';

/**
 * Common mapper for data transformations.
 * @returns Functions to convert datas.
 */
export function useCommonMapper() {
  const { t } = useI18n();

  /**
   * Convert ISO date to date with local date time format.
   * @param value Date string in ISO format (e.g., "2024-06-30T12:34:56.789Z") to be converted to a human-readable format.
   * @param format Optional format key to specify the desired date format from the i18n translations (default is "dateTimeFormat").
   * @returns Formatted date string according to the application's locale settings, or "-" if the input is invalid or falsy.
   */
  const toDate = (
    value: unknown,
    format: string = 'dateTimeFormat'
  ): string => {
    if (!value) {
      return '-';
    }
    const date = dayjs(value.toString());
    if (!date.isValid()) {
      return '-';
    }
    return date.format(t(`application.${format}`));
  };

  /**
   * Convert date format to iso.
   * @param value Date string to be converted to ISO format.
   * @returns String in ISO format (e.g., "2024-06-30T12:34:56.789Z") or an empty string if the input is falsy.
   */
  const toDateISO = (value: unknown): string => {
    const v = value?.toString() || '';
    if (!v) {
      return '';
    }
    const date = new Date(v);
    if (Number.isNaN(date.getTime())) {
      return '';
    }
    return date.toISOString();
  };

  return {
    toDate,
    toDateISO,
  };
}
