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
import { useAccountMapper } from 'src/composables/useAccountMapper';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useI18n } from 'vue-i18n';

const SPRING_QUERY_DATE_FORMAT = 'dd/MM/yyyy HH:mm:ss';

vi.mock('vue-i18n', () => ({
  useI18n: vi.fn(),
}));

describe('Test mapper: useAccountMapper', () => {
  const tMock = vi.fn();

  beforeEach(() => {
    tMock.mockReset();
    useI18n.mockReturnValue({ t: tMock });
  });

  const buildDto = (overrides = {}) => ({
    id: 1,
    lastname: 'Doe',
    firstname: 'John',
    email: 'john.doe@example.com',
    externalId: 'ext-1',
    createdBy: 'admin',
    updatedBy: 'admin2',
    insertDate: '2024-01-01T00:00:00Z',
    updateDate: '2024-02-01T00:00:00Z',
    status: 'ACTIVE',
    validityPeriod: { start: '2024-01-01T00:00:00Z', end: null },
    suspensionPeriod: { start: null, end: null },
    activationAt: '2024-01-02T00:00:00Z',
    statusReason: null,
    statusSubreason: null,
    statusComment: null,
    daysBeforeDeactivation: null,
    ...overrides,
  });

  describe('Test function: toAccount', () => {
    it('Map an AccountDTO to Account without lifecycle detail', () => {
      tMock.mockReturnValue('YYYY-MM-DD HH:mm');
      const { toAccount } = useAccountMapper();

      const dto = buildDto();
      const account = toAccount(dto);

      expect(account).toEqual({
        id: dto.id,
        externalId: dto.externalId,
        lastname: dto.lastname,
        firstname: dto.firstname,
        email: dto.email,
        createdBy: dto.createdBy,
        updatedBy: dto.updatedBy,
        insertDate: dayjs(dto.insertDate).format('YYYY-MM-DD HH:mm'),
        updateDate: dayjs(dto.updateDate).format('YYYY-MM-DD HH:mm'),
      });
      expect(account).not.toHaveProperty('status');
      expect(account).not.toHaveProperty('validityPeriod');
      expect(account).not.toHaveProperty('suspensionPeriod');
      expect(account).not.toHaveProperty('activationAt');
      expect(account).not.toHaveProperty('daysBeforeDeactivation');
    });
  });

  describe('Test function: toAccountStatus', () => {
    it('Map an AccountDTO to AccountStatus exposing only lifecycle fields', () => {
      const { toAccountStatus } = useAccountMapper();

      const dto = buildDto({
        status: 'SUSPENDED',
        validityPeriod: {
          start: '2024-01-01T00:00:00Z',
          end: '2025-01-01T00:00:00Z',
        },
        suspensionPeriod: {
          start: '2024-06-01T00:00:00Z',
          end: '2024-09-01T00:00:00Z',
        },
        statusReason: 'INVESTIGATION',
        statusSubreason: 'FRAUD',
        statusComment: 'Pending review',
        daysBeforeDeactivation: 42,
      });

      const accountStatus = toAccountStatus(dto);

      expect(accountStatus).toEqual({
        status: dto.status,
        validityPeriod: dto.validityPeriod,
        suspensionPeriod: dto.suspensionPeriod,
        activationAt: dto.activationAt,
        daysBeforeDeactivation: dto.daysBeforeDeactivation,
      });
      expect(accountStatus).not.toHaveProperty('statusReason');
      expect(accountStatus).not.toHaveProperty('statusSubreason');
      expect(accountStatus).not.toHaveProperty('statusComment');
      expect(accountStatus).not.toHaveProperty('id');
      expect(accountStatus).not.toHaveProperty('firstname');
      expect(accountStatus).not.toHaveProperty('lastname');
      expect(accountStatus).not.toHaveProperty('email');
      expect(accountStatus).not.toHaveProperty('insertDate');
      expect(accountStatus).not.toHaveProperty('updateDate');
    });

    it('Preserve null lifecycle values', () => {
      const { toAccountStatus } = useAccountMapper();

      const dto = buildDto({
        status: 'INACTIVE',
        activationAt: null,
        validityPeriod: { start: null, end: null },
        suspensionPeriod: { start: null, end: null },
      });

      const accountStatus = toAccountStatus(dto);

      expect(accountStatus.activationAt).toBeNull();
      expect(accountStatus.validityPeriod).toEqual({ start: null, end: null });
      expect(accountStatus.suspensionPeriod).toEqual({
        start: null,
        end: null,
      });
      expect(accountStatus.daysBeforeDeactivation).toBeNull();
    });
  });

  describe('Test function: toAccountQueryFilterDTO', () => {
    it('Convert filters to AccountQueryFilter', () => {
      const { toAccountQueryFilterDTO } = useAccountMapper();

      // The date field already applies the locale mask, so the value is
      // already formatted in the same `dateFormat` declared in the i18n
      // translations (here "DD/MM/YYYY").
      const filters = {
        firstname: 'John',
        lastname: 'doe',
        email: 'john.doe',
        createdBy: 'admin',
        insertDate: '24/07/2025',
      };

      const result = toAccountQueryFilterDTO(filters);

      expect(result.lastname).toEqual([`lk_*${filters.lastname}*`]);
      expect(result.firstname).toEqual([`lk_*${filters.firstname}*`]);
      expect(result.email).toEqual([`lk_*${filters.email}*`]);
      expect(result.createdBy).toEqual([`lk_*${filters.createdBy}*`]);
      expect(result.insertDate).toEqual([
        `${filters.insertDate} 00:00:00_bt_${filters.insertDate} 23:59:59`,
      ]);
      expect(result.dateFormat).toBe(SPRING_QUERY_DATE_FORMAT);
    });

    it('Preserves locale-ambiguous dates instead of swapping day and month', () => {
      const { toAccountQueryFilterDTO } = useAccountMapper();

      // Without forwarding the value as-is, dayjs would re-parse "04/05/2026"
      // as 5 April (MM/DD) and produce "05/04/2026" in the filter.
      const result = toAccountQueryFilterDTO({ insertDate: '04/05/2026' });

      expect(result.insertDate).toEqual([
        '04/05/2026 00:00:00_bt_04/05/2026 23:59:59',
      ]);
    });

    it('Handle empty filters', () => {
      const { toAccountQueryFilterDTO } = useAccountMapper();
      tMock.mockReturnValue('DD/MM/YYYY');

      const result = toAccountQueryFilterDTO({});

      expect(result.lastname).toBeNull();
      expect(result.firstname).toBeNull();
      expect(result.email).toBeNull();
      expect(result.createdBy).toBeNull();
      expect(result.insertDate).toBeNull();
      expect(result.dateFormat).toBe(SPRING_QUERY_DATE_FORMAT);
    });
  });

  describe('Test function: toAccountSuspensionRecord', () => {
    it('should map form fields into the suspension record structure', () => {
      const { toAccountSuspensionRecord } = useAccountMapper();

      const formData = {
        suspensionPeriodStart: '2026-07-01T00:00:00.000Z',
        suspensionPeriodEnd: '2026-09-01T00:00:00.000Z',
        statusReason: 'INVESTIGATION',
        statusSubreason: 'FRAUD',
        statusComment: 'Pending review',
      };

      const result = toAccountSuspensionRecord(formData);

      expect(result).toEqual({
        suspensionPeriod: {
          start: formData.suspensionPeriodStart,
          end: formData.suspensionPeriodEnd,
        },
        reason: formData.statusReason,
        subreason: formData.statusSubreason,
        comment: formData.statusComment,
      });
    });

    it('should coerce empty and undefined form fields to null', () => {
      const { toAccountSuspensionRecord } = useAccountMapper();

      const result = toAccountSuspensionRecord({});

      expect(result).toEqual({
        suspensionPeriod: { start: null, end: null },
        reason: '',
        subreason: '',
        comment: null,
      });
    });
  });

  describe('Test function: toAccountDeactivationRecord', () => {
    it('should map the validity end into the deactivation timestamp', () => {
      const { toAccountDeactivationRecord } = useAccountMapper();

      const formData = {
        validityPeriodEnd: '2027-06-01T00:00:00.000Z',
        statusReason: 'OFFBOARDING',
        statusSubreason: 'CONTRACT_END',
        statusComment: 'End of contract',
      };

      const result = toAccountDeactivationRecord(formData);

      expect(result).toEqual({
        deactivationAt: formData.validityPeriodEnd,
        reason: formData.statusReason,
        subreason: formData.statusSubreason,
        comment: formData.statusComment,
      });
    });

    it('should coerce empty fields to empty string / null', () => {
      const { toAccountDeactivationRecord } = useAccountMapper();

      const result = toAccountDeactivationRecord({});

      expect(result).toEqual({
        deactivationAt: '',
        reason: '',
        subreason: '',
        comment: null,
      });
    });
  });

  describe('Test function: toAccountReactivationRecord', () => {
    it('should map the comment into the reactivation record', () => {
      const { toAccountReactivationRecord } = useAccountMapper();

      const result = toAccountReactivationRecord({
        statusComment: 'Cleared after review',
      });

      expect(result).toEqual({ comment: 'Cleared after review' });
    });

    it('should coerce a missing comment to an empty string', () => {
      const { toAccountReactivationRecord } = useAccountMapper();

      expect(toAccountReactivationRecord({})).toEqual({ comment: '' });
    });

    it('should add the validity end when re-validating a deactivated account', () => {
      const { toAccountReactivationRecord } = useAccountMapper();

      const result = toAccountReactivationRecord({
        statusComment: 'Re-validated after appeal',
        validityPeriodEnd: '2099-12-31T00:00:00.000Z',
      });

      expect(result).toEqual({
        comment: 'Re-validated after appeal',
        validityEnd: '2099-12-31T00:00:00.000Z',
      });
    });

    it('should omit the validity end when no end date is provided', () => {
      const { toAccountReactivationRecord } = useAccountMapper();

      const result = toAccountReactivationRecord({
        statusComment: 'Lifting suspension only',
      });

      expect(result).toEqual({ comment: 'Lifting suspension only' });
      expect(result).not.toHaveProperty('validityEnd');
    });
  });

  describe('Test function: toAccountValidityRecord', () => {
    it('should map the validity start into the validity record', () => {
      const { toAccountValidityRecord } = useAccountMapper();

      const result = toAccountValidityRecord({
        validityPeriodStart: '2026-08-01T00:00:00.000Z',
      });

      expect(result).toEqual({ validityStart: '2026-08-01T00:00:00.000Z' });
    });

    it('should coerce a missing validity start to an empty string', () => {
      const { toAccountValidityRecord } = useAccountMapper();

      expect(toAccountValidityRecord({})).toEqual({ validityStart: '' });
    });
  });

  describe('Test function: toAccountList', () => {
    it('Map an array of AccountDTOs to Accounts', () => {
      tMock.mockReturnValue('YYYY-MM-DD HH:mm');
      const { toAccountList } = useAccountMapper();

      const dtos = [
        buildDto({ id: 1 }),
        buildDto({
          id: 2,
          lastname: 'Smith',
          firstname: 'Jane',
          email: 'jane.smith@example.com',
          externalId: 'ext-2',
          updatedBy: 'admin',
          insertDate: '2024-03-01T00:00:00Z',
          updateDate: '2024-04-01T00:00:00Z',
        }),
      ];

      const accounts = toAccountList(dtos);

      expect(accounts).toHaveLength(2);
      expect(accounts[0]).toEqual({
        id: dtos[0].id,
        externalId: dtos[0].externalId,
        lastname: dtos[0].lastname,
        firstname: dtos[0].firstname,
        email: dtos[0].email,
        createdBy: dtos[0].createdBy,
        updatedBy: dtos[0].updatedBy,
        insertDate: dayjs(dtos[0].insertDate).format('YYYY-MM-DD HH:mm'),
        updateDate: dayjs(dtos[0].updateDate).format('YYYY-MM-DD HH:mm'),
      });
      expect(accounts[1]).toEqual({
        id: dtos[1].id,
        externalId: dtos[1].externalId,
        lastname: dtos[1].lastname,
        firstname: dtos[1].firstname,
        email: dtos[1].email,
        createdBy: dtos[1].createdBy,
        updatedBy: dtos[1].updatedBy,
        insertDate: dayjs(dtos[1].insertDate).format('YYYY-MM-DD HH:mm'),
        updateDate: dayjs(dtos[1].updateDate).format('YYYY-MM-DD HH:mm'),
      });
    });

    it('Return empty array when given an empty list', () => {
      const { toAccountList } = useAccountMapper();
      expect(toAccountList([])).toEqual([]);
    });
  });
});
