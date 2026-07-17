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

import type { LinidAttributeConfiguration } from '@linagora/linid-im-front-corelib';
import { dayjs } from 'boot/dayjs';
import type { Dayjs } from 'dayjs';
import { useI18n } from 'vue-i18n';

/**
 * Common mapper for data transformations.
 *
 * @returns Functions to convert datas.
 */
export function useCommonMapper() {
  const { t } = useI18n();

  /**
   * Convert ISO date to date with local date time format.
   *
   * @param value Date string in ISO format (e.g., "2024-06-30T12:34:56.789Z") to be converted to a human-readable
   *   format.
   * @param formatKey Optional format key to specify the desired date format from the i18n translations (default is
   *   "dateFormat").
   * @returns Formatted date string according to the application's locale settings, or "" if the input is invalid or
   *   falsy.
   */
  const toDate = (value: unknown, formatKey: string = 'dateFormat'): string => {
    if (!value) {
      return '';
    }
    const date = dayjs(value.toString());
    if (!date.isValid()) {
      return '';
    }
    return date.format(t(`application.${formatKey}`));
  };

  /**
   * Matches a strict ISO 8601 datetime string of the form `YYYY-MM-DDThh:mm:ss[.sss](Z|±hh:mm)`.
   *
   * - `^\d{4}-\d{2}-\d{2}` — date part: four-digit year, two-digit month, two-digit day, separated by hyphens.
   * - `T\d{2}:\d{2}:\d{2}` — time part: two-digit hour, minute, and second, separated by colons, preceded by the literal
   *   `T`.
   * - `(\.\d+)?` — optional fractional seconds (one or more digits after a dot).
   * - `(Z|[+-]\d{2}:\d{2})$` — mandatory timezone: either the UTC designator `Z`, or a signed offset in `±hh:mm` form.
   */
  const ISO_REGEX =
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:\d{2})$/;

  /**
   * Convert a date string to a canonical ISO 8601 UTC string. If the input matches the ISO 8601 datetime pattern
   * (detected via regex), it is parsed by dayjs, validated, and normalized to a UTC ISO string. If the input does not
   * match the ISO pattern, strict parsing is applied using the locale format retrieved from `application.${formatKey}`
   * (e.g., "DD/MM/YYYY").
   *
   * @param value Date string to be converted to ISO format for API consumption. The value must either be a valid ISO
   *   8601 string, or match the format pattern retrieved from `application.${formatKey}`; strict parsing is enforced in
   *   both cases, so any value that does not exactly match the expected format or is semantically invalid will return
   *   an empty string.
   * @param formatKey Optional format key to specify the expected input date format from the i18n translations (default
   *   is "dateFormat").
   * @returns Canonical string in ISO 8601 UTC format (e.g., "2024-06-30T00:00:00.000Z"), or an empty string if the
   *   input is falsy, does not match the expected format, or represents an invalid date.
   */
  const toDateISO = (
    value: unknown,
    formatKey: string = 'dateFormat'
  ): string => {
    const v = value?.toString() || '';
    if (!v) {
      return '';
    }

    if (ISO_REGEX.test(v)) {
      const date = dayjs(v);
      return date.isValid() ? v : '';
    }

    const date = dayjs.utc(v, t(`application.${formatKey}`), true);
    return date.isValid() ? date.toISOString() : '';
  };

  /**
   * Builds an empty record where every field in {@link fields} is initialised to `null`. Use this to seed a reactive
   * form object whose shape is driven by runtime configuration.
   *
   * @param fields - Attribute definitions that describe the form fields.
   * @returns A plain object with one `null`-valued key per field name.
   */
  const toEmptyRecord = <T>(fields: LinidAttributeConfiguration[]): T => {
    return Object.fromEntries(
      fields.map((field) => [field.name, ''])
    ) as unknown as T;
  };

  /**
   * Parses a date value into a {@link Dayjs} object. Returns null when the input is falsy, the placeholder "-", or
   * cannot be parsed by dayjs.
   *
   * @param value Date value to parse (typically an ISO 8601 string from the API).
   * @returns A Dayjs object, or null when the input is invalid or absent.
   */
  const toDayJs = (value: unknown): Dayjs | null => {
    const v = value?.toString() || '';
    if (v === '') {
      return null;
    }
    const date = dayjs(v);
    return date.isValid() ? date : null;
  };

  return {
    toDate,
    toDateISO,
    toEmptyRecord,
    toDayJs,
  };
}
