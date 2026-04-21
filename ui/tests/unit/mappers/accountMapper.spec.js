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
import { useAccountMapper } from 'src/mappers/accountMapper';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useI18n } from 'vue-i18n';

const SPRING_QUERY_DATE_FORMAT = 'dd/MM/yyyy HH:mm:ss';

vi.mock('vue-i18n', () => ({
  useI18n: vi.fn(),
}));

describe('accountMapper', () => {
  const tMock = vi.fn();

  beforeEach(() => {
    tMock.mockReset();
    useI18n.mockReturnValue({ t: tMock });
  });

  describe('toAccount', () => {
    it('Map an AccountDTO to Account', () => {
      tMock.mockReturnValue('YYYY-MM-DD HH:mm');
      const { toAccount } = useAccountMapper();

      const dto = {
        id: 1,
        lastname: 'Doe',
        firstname: 'John',
        email: 'john.doe@example.com',
        externalId: 'ext-1',
        createdBy: 'admin',
        updatedBy: 'admin2',
        insertDate: '2024-01-01T00:00:00Z',
        updateDate: '2024-02-01T00:00:00Z',
      };

      const account = toAccount(dto);
      expect(account).toEqual({
        ...dto,
        insertDate: dayjs(dto.insertDate).format('YYYY-MM-DD HH:mm'),
        updateDate: dayjs(dto.updateDate).format('YYYY-MM-DD HH:mm'),
      });
    });
  });

  describe('Test function: toAccountQueryFilterDTO', () => {
    it('Convert filters to AccountQueryFilter', () => {
      const { toAccountQueryFilterDTO } = useAccountMapper();
      tMock.mockReturnValue('DD/MM/YYYY');

      const filters = {
        firstname: 'John',
        lastname: 'doe',
        email: 'john.doe',
        createdBy: 'admin',
        insertDate: '2025-07-24T00:00:00Z',
      };

      const result = toAccountQueryFilterDTO(filters);

      expect(result.lastname).toEqual([`lk_*${filters.lastname}*`]);
      expect(result.firstname).toEqual([`lk_*${filters.firstname}*`]);
      expect(result.email).toEqual([`lk_*${filters.email}*`]);
      expect(result.createdBy).toEqual([`lk_*${filters.createdBy}*`]);
      const formattedDate = dayjs(filters.insertDate).format('DD/MM/YYYY');
      expect(result.insertDate).toEqual([
        `${formattedDate} 00:00:00_bt_${formattedDate} 23:59:59`,
      ]);
      expect(result.dateFormat).toBe(SPRING_QUERY_DATE_FORMAT);
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

  describe('Test function: toAccountList', () => {
    it('Map an array of AccountDTOs to Accounts', () => {
      tMock.mockReturnValue('YYYY-MM-DD HH:mm');
      const { toAccountList } = useAccountMapper();

      const dtos = [
        {
          id: 1,
          lastname: 'Doe',
          firstname: 'John',
          email: 'john.doe@example.com',
          externalId: 'ext-1',
          createdBy: 'admin',
          updatedBy: 'admin2',
          insertDate: '2024-01-01T00:00:00Z',
          updateDate: '2024-02-01T00:00:00Z',
        },
        {
          id: 2,
          lastname: 'Smith',
          firstname: 'Jane',
          email: 'jane.smith@example.com',
          externalId: 'ext-2',
          createdBy: 'admin',
          updatedBy: 'admin',
          insertDate: '2024-03-01T00:00:00Z',
          updateDate: '2024-04-01T00:00:00Z',
        },
      ];

      const accounts = toAccountList(dtos);

      expect(accounts).toHaveLength(2);
      expect(accounts[0]).toEqual({
        ...dtos[0],
        insertDate: dayjs(dtos[0].insertDate).format('YYYY-MM-DD HH:mm'),
        updateDate: dayjs(dtos[0].updateDate).format('YYYY-MM-DD HH:mm'),
      });
      expect(accounts[1]).toEqual({
        ...dtos[1],
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
