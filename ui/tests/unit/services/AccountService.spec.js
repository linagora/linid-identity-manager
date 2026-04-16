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
import { getAccountById } from 'src/services/AccountService';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('boot/axios', () => ({
  api: {
    get: vi.fn(),
  },
}));

vi.mock('src/mappers/accountMapper', () => ({
  useAccountMapper: () => ({
    toAccount: vi.fn((dto) => ({
      ...dto,
      insertDate: 'mapped-date',
      updateDate: 'mapped-date',
    })),
  }),
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
  ...overrides,
});

describe('Test service: accountService', () => {
  beforeEach(() => {
    vi.mocked(api.get).mockReset();
  });

  describe('Test function: getAccountById', () => {
    it('should call valid endpoint and return mapped account data', async () => {
      const dto = buildAccountDTO({ id: USER_1_UUID });
      vi.mocked(api.get).mockResolvedValue({ data: dto });

      const result = await getAccountById(USER_1_UUID);

      expect(api.get).toHaveBeenCalledWith(`/accounts/${USER_1_UUID}`);
      expect(result).toEqual({
        ...dto,
        insertDate: 'mapped-date',
        updateDate: 'mapped-date',
      });
    });
  });
});
