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
import { useCommonMapper } from 'src/mappers/commonMapper';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useI18n } from 'vue-i18n';

vi.mock('vue-i18n', () => ({
  useI18n: vi.fn(),
}));

describe('Test mapper: commonMapper', () => {
  const tMock = vi.fn();

  beforeEach(() => {
    tMock.mockReset();
    useI18n.mockReturnValue({ t: tMock });
  });

  describe('Test function: toDate', () => {
    it('should return "-" for falsy values (null/undefined/empty)', () => {
      tMock.mockReturnValue('YYYY-MM-DD HH:mm');
      const { toDate } = useCommonMapper();

      expect(toDate(null)).toBe('-');
      expect(toDate(undefined)).toBe('-');
      expect(toDate('')).toBe('-');
    });

    it('should return "-" for an invalid date string', () => {
      tMock.mockReturnValue('YYYY-MM-DD HH:mm');
      const { toDate } = useCommonMapper();

      expect(toDate('not-a-valid-date')).toBe('-');
    });

    it('should format a valid ISO date string', () => {
      tMock.mockReturnValue('YYYY-MM-DD');
      const { toDate } = useCommonMapper();

      const iso = '2024-07-20T12:34:56.000Z';
      const result = toDate(iso);

      expect(result).toBe(dayjs(iso).format('YYYY-MM-DD'));
      expect(tMock).toHaveBeenCalledWith('application.dateFormat');
    });
  });

  describe('Test function: toDateISO', () => {
    it('should return "" for falsy values (null/undefined/empty)', () => {
      const { toDateISO } = useCommonMapper();

      expect(toDateISO(null)).toBe('');
      expect(toDateISO(undefined)).toBe('');
      expect(toDateISO('')).toBe('');
    });

    it('should return "" for an invalid date string', () => {
      const { toDateISO } = useCommonMapper();

      expect(toDateISO('not-a-valid-date')).toBe('');
    });

    it('should return "" for an ISO string that does not match the locale format', () => {
      tMock.mockReturnValue('DD/MM/YYYY');
      const { toDateISO } = useCommonMapper();

      expect(toDateISO('2024-06-30T00:00:00.000Z')).toBe('');
    });

    it('should convert a valid date string to ISO format', () => {
      const { toDateISO } = useCommonMapper();

      const input = '2025-07-24T00:00:00Z';
      const result = toDateISO(input);

      expect(result).toBe('2025-07-24T00:00:00.000Z');
    });
  });

  describe('Test function: toEmptyRecord', () => {
    it('should return a record with every declared field set to an empty string', () => {
      const { toEmptyRecord } = useCommonMapper();
      const fields = [
        { name: 'externalId', type: 'text', rules: ['required'] },
        { name: 'email', type: 'email', rules: ['required', 'email'] },
      ];

      expect(toEmptyRecord(fields)).toEqual({
        externalId: '',
        email: '',
      });
    });

    it('should return an empty object for an empty field list', () => {
      const { toEmptyRecord } = useCommonMapper();

      expect(toEmptyRecord([])).toEqual({});
    });

    it('should return a new object on each call', () => {
      const { toEmptyRecord } = useCommonMapper();
      const fields = [{ name: 'foo', type: 'text', rules: [] }];

      expect(toEmptyRecord(fields)).not.toBe(toEmptyRecord(fields));
    });
  });

  describe('Test function: toLikeFilter', () => {
    it('should format string values with lk_*value*', () => {
      const { toLikeFilter } = useCommonMapper();

      expect(toLikeFilter('John')).toEqual(['lk_*John*']);
      expect(toLikeFilter(123)).toEqual(['lk_*123*']);
    });

    it('should return null for falsy values (null/undefined/empty)', () => {
      const { toLikeFilter } = useCommonMapper();

      expect(toLikeFilter(null)).toBe(null);
      expect(toLikeFilter(undefined)).toBe(null);
      expect(toLikeFilter('')).toBe(null);
    });
  });

  describe('Test function: toDateFilter', () => {
    it('should format a date value as a between filter (start of day to end of day)', () => {
      const { toDateFilter } = useCommonMapper();

      const value = '24/07/2025';
      expect(toDateFilter(value)).toEqual([
        `${value} 00:00:00_bt_${value} 23:59:59`,
      ]);
    });

    it('should return null for falsy values (null/undefined/empty)', () => {
      const { toDateFilter } = useCommonMapper();

      expect(toDateFilter(null)).toBe(null);
      expect(toDateFilter(undefined)).toBe(null);
      expect(toDateFilter('')).toBe(null);
    });
  });

  describe('Test function: toDayJs', () => {
    it('should parse an ISO 8601 string into a Dayjs object', () => {
      const { toDayJs } = useCommonMapper();

      const result = toDayJs('2025-07-24T12:34:56.000Z');

      expect(result?.isValid()).toBe(true);
      expect(result?.toISOString()).toBe('2025-07-24T12:34:56.000Z');
    });

    it('should return null for falsy values (null/undefined/empty)', () => {
      const { toDayJs } = useCommonMapper();

      expect(toDayJs(null)).toBeNull();
      expect(toDayJs(undefined)).toBeNull();
      expect(toDayJs('')).toBeNull();
    });

    it('should return null for the placeholder "-"', () => {
      const { toDayJs } = useCommonMapper();

      expect(toDayJs('-')).toBeNull();
    });

    it('should return null for an invalid date string', () => {
      const { toDayJs } = useCommonMapper();

      expect(toDayJs('not-a-valid-date')).toBeNull();
    });
  });
});
