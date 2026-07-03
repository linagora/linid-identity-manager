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

import { api } from 'boot/axios';
import {
  deactivateAccount,
  getAccountById,
  getAccounts,
  reactivateAccount,
  setAccountValidity,
  suspendAccount,
} from 'src/services/AccountService';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('boot/axios', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

const USER_1_UUID = '11111111-1111-4111-8111-111111111111';

const buildAccountDTO = (overrides = {}) => ({
  id: '00000000-0000-4000-8000-000000000000',
  externalId: 'external-id',
  firstname: 'Test',
  lastname: 'Account',
  email: 'test.account@example.com',
  createdBy: USER_1_UUID,
  updatedBy: USER_1_UUID,
  insertDate: '2026-04-15T12:00:00.000000Z',
  updateDate: '2026-04-15T12:00:00.000000Z',
  status: 'ACTIVE',
  validityPeriod: { start: '2026-01-01T00:00:00Z', end: null },
  suspensionPeriod: { start: null, end: null },
  activationAt: '2026-01-01T00:00:00Z',
  statusReason: null,
  statusSubreason: null,
  statusComment: null,
  daysBeforeDeactivation: null,
  ...overrides,
});

describe('Test service: accountService', () => {
  beforeEach(() => {
    vi.mocked(api.get).mockReset();
    vi.mocked(api.put).mockReset();
  });

  describe('Test function: getAccountById', () => {
    it('should call valid endpoint and return the raw DTO', async () => {
      const dto = buildAccountDTO({ id: USER_1_UUID });
      vi.mocked(api.get).mockResolvedValue({ data: dto });

      const result = await getAccountById(USER_1_UUID);

      expect(api.get).toHaveBeenCalledWith(`/accounts/${USER_1_UUID}`);
      expect(result).toEqual(dto);
    });

    it('should propagate backend errors to the caller', async () => {
      const error = new Error('boom');
      vi.mocked(api.get).mockRejectedValue(error);

      await expect(getAccountById(USER_1_UUID)).rejects.toThrow('boom');
    });
  });

  describe('Test function: getAccounts', () => {
    const filters = {
      lastname: ['lk_*doe*'],
      firstname: null,
      email: null,
      createdBy: null,
      insertDate: null,
      dateFormat: 'dd/MM/yyyy HH:mm:ss',
    };
    const pagination = { page: 0, size: 10, sort: 'lastname,asc' };

    it('should call valid endpoint with merged params and return the page', async () => {
      const dto = buildAccountDTO({ id: USER_1_UUID });
      const page = {
        content: [dto],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
      };
      vi.mocked(api.get).mockResolvedValue({ data: page });

      const result = await getAccounts(filters, pagination);

      expect(api.get).toHaveBeenCalledWith('/accounts', {
        params: { ...filters, ...pagination },
      });
      expect(result).toEqual(page);
    });

    it('should propagate backend errors to the caller', async () => {
      const error = new Error('boom');
      vi.mocked(api.get).mockRejectedValue(error);

      await expect(getAccounts(filters, pagination)).rejects.toThrow('boom');
    });
  });

  describe('Test function: suspendAccount', () => {
    const payload = {
      suspensionPeriod: { start: '2026-06-01T00:00:00Z', end: null },
      reason: 'INVESTIGATION',
      subreason: null,
      comment: null,
    };

    it('should call the suspension endpoint with payload and return the updated DTO', async () => {
      const dto = buildAccountDTO({ id: USER_1_UUID, status: 'SUSPENDED' });
      vi.mocked(api.put).mockResolvedValue({ data: dto });

      const result = await suspendAccount(USER_1_UUID, payload);

      expect(api.put).toHaveBeenCalledWith(
        `/accounts/${USER_1_UUID}/status/suspend`,
        payload
      );
      expect(result).toEqual(dto);
    });

    it('should propagate backend errors to the caller', async () => {
      vi.mocked(api.put).mockRejectedValue(new Error('boom'));

      await expect(suspendAccount(USER_1_UUID, payload)).rejects.toThrow(
        'boom'
      );
    });
  });

  describe('Test function: deactivateAccount', () => {
    const payload = {
      deactivationAt: '2026-07-01T00:00:00Z',
      reason: 'OFFBOARDING',
      subreason: null,
      comment: null,
    };

    it('should call the deactivation endpoint with payload and return the updated DTO', async () => {
      const dto = buildAccountDTO({ id: USER_1_UUID, status: 'INACTIVE' });
      vi.mocked(api.put).mockResolvedValue({ data: dto });

      const result = await deactivateAccount(USER_1_UUID, payload);

      expect(api.put).toHaveBeenCalledWith(
        `/accounts/${USER_1_UUID}/status/deactivate`,
        payload
      );
      expect(result).toEqual(dto);
    });

    it('should propagate backend errors to the caller', async () => {
      vi.mocked(api.put).mockRejectedValue(new Error('boom'));

      await expect(deactivateAccount(USER_1_UUID, payload)).rejects.toThrow(
        'boom'
      );
    });
  });

  describe('Test function: reactivateAccount', () => {
    const payload = { comment: 'Investigation closed' };

    it('should call the reactivation endpoint with payload and return the updated DTO', async () => {
      const dto = buildAccountDTO({ id: USER_1_UUID, status: 'ACTIVE' });
      vi.mocked(api.put).mockResolvedValue({ data: dto });

      const result = await reactivateAccount(USER_1_UUID, payload);

      expect(api.put).toHaveBeenCalledWith(
        `/accounts/${USER_1_UUID}/status/reactivate`,
        payload
      );
      expect(result).toEqual(dto);
    });

    it('should propagate backend errors to the caller', async () => {
      vi.mocked(api.put).mockRejectedValue(new Error('boom'));

      await expect(reactivateAccount(USER_1_UUID, payload)).rejects.toThrow(
        'boom'
      );
    });
  });

  describe('Test function: setAccountValidity', () => {
    const payload = { validityStart: '2026-08-01T00:00:00Z' };

    it('should call the validity endpoint with payload and return the updated DTO', async () => {
      const dto = buildAccountDTO({ id: USER_1_UUID, status: 'INACTIVE' });
      vi.mocked(api.put).mockResolvedValue({ data: dto });

      const result = await setAccountValidity(USER_1_UUID, payload);

      expect(api.put).toHaveBeenCalledWith(
        `/accounts/${USER_1_UUID}/status/schedule-activation`,
        payload
      );
      expect(result).toEqual(dto);
    });

    it('should propagate backend errors to the caller', async () => {
      vi.mocked(api.put).mockRejectedValue(new Error('boom'));

      await expect(setAccountValidity(USER_1_UUID, payload)).rejects.toThrow(
        'boom'
      );
    });
  });
});
