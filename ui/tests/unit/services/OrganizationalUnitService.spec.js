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
  createOrganizationalUnit,
  getAccountsByOrganizationalUnitId,
  getAllOrganizationalUnit,
  getOrganizationalUnitById,
  getOrganizationalUnits,
  updateOrganizationalUnitStatus,
} from 'src/services/OrganizationalUnitService';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('boot/config', () => ({
  appConfig: {
    organizationalUnitQuerySize: 50,
  },
}));

vi.mock('boot/axios', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

const ROOT_UUID = '00000000-0000-4000-8000-000000000000';
const OU_UUID = '11111111-1111-4111-8111-111111111111';
const ACCOUNT_UUID = '22222222-2222-4222-8222-222222222222';

const buildOuDTO = (overrides = {}) => ({
  id: OU_UUID,
  name: 'Engineering',
  type: 'DEPARTMENT',
  createdBy: ROOT_UUID,
  updatedBy: ROOT_UUID,
  insertDate: '2026-05-13T12:00:00.000000Z',
  updateDate: '2026-05-13T12:00:00.000000Z',
  suspensionPeriod: null,
  statusReason: null,
  statusSubreason: null,
  statusComment: null,
  isSuspended: false,
  parents: [],
  ...overrides,
});

const buildAccountDTO = (overrides = {}) => ({
  id: ACCOUNT_UUID,
  externalId: 'user1',
  firstname: 'User',
  lastname: 'One',
  email: 'user1@example.com',
  status: 'ACTIVE',
  ...overrides,
});

const buildPage = (content, overrides = {}) => ({
  content,
  totalElements: content.length,
  totalPages: 1,
  size: 50,
  number: 0,
  last: true,
  first: true,
  ...overrides,
});

describe('Test service: organizationalUnitService', () => {
  beforeEach(() => {
    vi.mocked(api.get).mockReset();
    vi.mocked(api.post).mockReset();
    vi.mocked(api.put).mockReset();
  });

  describe('Test function: getOrganizationalUnitById', () => {
    it('should call valid endpoint and return the raw DTO', async () => {
      const dto = buildOuDTO();
      vi.mocked(api.get).mockResolvedValue({ data: dto });

      const result = await getOrganizationalUnitById(OU_UUID);

      expect(api.get).toHaveBeenCalledWith(`/organizational-units/${OU_UUID}`);
      expect(result).toEqual(dto);
    });

    it('should propagate backend errors to the caller', async () => {
      const error = new Error('boom');
      vi.mocked(api.get).mockRejectedValue(error);

      await expect(getOrganizationalUnitById(OU_UUID)).rejects.toThrow('boom');
    });
  });

  describe('Test function: createOrganizationalUnit', () => {
    const payload = {
      parent: ROOT_UUID,
      name: 'Engineering',
      type: 'DEPARTMENT',
    };

    it('should call valid endpoint with payload and return the raw DTO', async () => {
      const dto = buildOuDTO();
      vi.mocked(api.post).mockResolvedValue({ data: dto });

      const result = await createOrganizationalUnit(payload);

      expect(api.post).toHaveBeenCalledWith('/organizational-units', payload);
      expect(result).toEqual(dto);
    });

    it('should propagate backend errors to the caller', async () => {
      const error = new Error('boom');
      vi.mocked(api.post).mockRejectedValue(error);

      await expect(createOrganizationalUnit(payload)).rejects.toThrow('boom');
    });
  });

  describe('Test function: getOrganizationalUnits', () => {
    const filters = { name: 'Eng' };
    const pagination = { page: 0, size: 10 };

    it('should call valid endpoint with filters and pagination and return the page', async () => {
      const page = buildPage([buildOuDTO()]);
      vi.mocked(api.get).mockResolvedValue({ data: page });

      const result = await getOrganizationalUnits(filters, pagination);

      expect(api.get).toHaveBeenCalledWith('/organizational-units', {
        params: { ...filters, ...pagination },
      });
      expect(result).toEqual(page);
    });

    it('should propagate backend errors to the caller', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('boom'));

      await expect(getOrganizationalUnits(filters, pagination)).rejects.toThrow(
        'boom'
      );
    });
  });

  describe('Test function: getAllOrganizationalUnit', () => {
    it('should return all results when there is a single page', async () => {
      const ou1 = buildOuDTO({ id: OU_UUID });
      const ou2 = buildOuDTO({ id: ROOT_UUID, name: 'HR' });
      const page = buildPage([ou1, ou2], { last: true });
      vi.mocked(api.get).mockResolvedValue({ data: page });

      const result = await getAllOrganizationalUnit();

      expect(api.get).toHaveBeenCalledTimes(1);
      expect(result).toEqual([ou1, ou2]);
    });

    it('should iterate through multiple pages and aggregate all results', async () => {
      const ou1 = buildOuDTO({ id: OU_UUID });
      const ou2 = buildOuDTO({ id: ROOT_UUID, name: 'HR' });
      const page1 = buildPage([ou1], { last: false, number: 0 });
      const page2 = buildPage([ou2], { last: true, number: 1 });

      vi.mocked(api.get)
        .mockResolvedValueOnce({ data: page1 })
        .mockResolvedValueOnce({ data: page2 });

      const result = await getAllOrganizationalUnit();

      expect(api.get).toHaveBeenCalledTimes(2);
      expect(api.get).toHaveBeenNthCalledWith(1, '/organizational-units', {
        params: { name: null, page: 0, size: 50 },
      });
      expect(api.get).toHaveBeenNthCalledWith(2, '/organizational-units', {
        params: { name: null, page: 1, size: 50 },
      });
      expect(result).toEqual([ou1, ou2]);
    });

    it('should return an empty array when the page is empty', async () => {
      const page = buildPage([], { last: true });
      vi.mocked(api.get).mockResolvedValue({ data: page });

      const result = await getAllOrganizationalUnit();

      expect(result).toEqual([]);
    });

    it('should propagate backend errors to the caller', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('boom'));

      await expect(getAllOrganizationalUnit()).rejects.toThrow('boom');
    });
  });

  describe('Test function: getAccountsByOrganizationalUnitId', () => {
    const filters = { email: 'user1@example.com' };
    const pagination = { page: 0, size: 10 };

    it('should call valid endpoint with id, filters and pagination and return the page', async () => {
      const page = buildPage([buildAccountDTO()]);
      vi.mocked(api.get).mockResolvedValue({ data: page });

      const result = await getAccountsByOrganizationalUnitId(
        OU_UUID,
        filters,
        pagination
      );

      expect(api.get).toHaveBeenCalledWith(
        `/organizational-units/${OU_UUID}/accounts`,
        {
          params: { ...filters, ...pagination },
        }
      );
      expect(result).toEqual(page);
    });

    it('should propagate backend errors to the caller', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('boom'));

      await expect(
        getAccountsByOrganizationalUnitId(OU_UUID, filters, pagination)
      ).rejects.toThrow('boom');
    });
  });

  describe('Test function: updateOrganizationalUnitStatus', () => {
    const payload = {
      suspensionPeriod: {
        start: '2026-06-01T00:00:00.000Z',
        end: '2026-07-01T00:00:00.000Z',
      },
      reason: 'AUDIT',
    };

    it('should PUT to the status endpoint and return the raw DTO', async () => {
      const dto = buildOuDTO({
        isSuspended: false,
        suspensionPeriod: payload.suspensionPeriod,
      });
      vi.mocked(api.put).mockResolvedValue({ data: dto });

      const result = await updateOrganizationalUnitStatus(OU_UUID, payload);

      expect(api.put).toHaveBeenCalledWith(
        `/organizational-units/${OU_UUID}/status`,
        payload
      );
      expect(result).toEqual(dto);
    });

    it('should propagate backend errors to the caller', async () => {
      const error = new Error('boom');
      vi.mocked(api.put).mockRejectedValue(error);

      await expect(
        updateOrganizationalUnitStatus(OU_UUID, payload)
      ).rejects.toThrow('boom');
    });
  });
});
