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
  getOrganizationalUnitById,
  reactivateOrganizationalUnit,
  suspendOrganizationalUnit,
} from 'src/services/OrganizationalUnitService';
import { beforeEach, describe, expect, it, vi } from 'vitest';

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

      expect(api.get).toHaveBeenCalledWith(`/organizational-units/${OU_UUID}`, {
        signal: undefined,
      });
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

  describe('Test function: suspendOrganizationalUnit', () => {
    const payload = {
      suspensionPeriod: {
        start: '2026-06-01T00:00:00.000Z',
        end: '2026-07-01T00:00:00.000Z',
      },
      reason: 'AUDIT',
      subreason: null,
      comment: null,
    };

    it('should PUT to the suspension endpoint and return the raw DTO', async () => {
      const dto = buildOuDTO({
        isSuspended: true,
        suspensionPeriod: payload.suspensionPeriod,
      });
      vi.mocked(api.put).mockResolvedValue({ data: dto });

      const result = await suspendOrganizationalUnit(OU_UUID, payload);

      expect(api.put).toHaveBeenCalledWith(
        `/organizational-units/${OU_UUID}/status/suspend`,
        payload
      );
      expect(result).toEqual(dto);
    });

    it('should propagate backend errors to the caller', async () => {
      vi.mocked(api.put).mockRejectedValue(new Error('boom'));

      await expect(suspendOrganizationalUnit(OU_UUID, payload)).rejects.toThrow(
        'boom'
      );
    });
  });

  describe('Test function: reactivateOrganizationalUnit', () => {
    const payload = { comment: 'Merger completed' };

    it('should PUT to the reactivation endpoint and return the raw DTO', async () => {
      const dto = buildOuDTO({ isSuspended: false });
      vi.mocked(api.put).mockResolvedValue({ data: dto });

      const result = await reactivateOrganizationalUnit(OU_UUID, payload);

      expect(api.put).toHaveBeenCalledWith(
        `/organizational-units/${OU_UUID}/status/reactivate`,
        payload
      );
      expect(result).toEqual(dto);
    });

    it('should propagate backend errors to the caller', async () => {
      vi.mocked(api.put).mockRejectedValue(new Error('boom'));

      await expect(
        reactivateOrganizationalUnit(OU_UUID, payload)
      ).rejects.toThrow('boom');
    });
  });
});
