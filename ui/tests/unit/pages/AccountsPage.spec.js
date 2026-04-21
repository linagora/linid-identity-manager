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

import { flushPromises, shallowMount } from '@vue/test-utils';
import { getAccounts } from 'src/services/AccountsService';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AccountsPage from '../../../src/pages/AccountsPage.vue';

const mockedGetAccounts = vi.mocked(getAccounts);

const mockRouter = {
  push: vi.fn(),
};

const mockRoute = {
  path: '/accounts',
};

const mockNotify = vi.fn();

vi.mock('@linagora/linid-im-front-corelib', () => ({
  loadAsyncComponent: vi.fn(() => null),
  useScopedI18n: () => ({
    t: vi.fn((v) => v),
  }),
  useUiDesign: () => ({
    ui: vi.fn(() => ({})),
  }),
  useNotify: () => ({
    Notify: mockNotify,
  }),
  usePagination: () => ({
    toPagination: vi.fn((pagination) => ({
      page: pagination.page - 1,
      size: pagination.rowsPerPage,
      sort: pagination.sortBy,
      direction: pagination.descending ? 'desc' : 'asc',
    })),
    toQuasarPagination: vi.fn((page) => ({
      page: (page.number ?? 0) + 1,
      rowsPerPage: page.size ?? 10,
      rowsNumber: page.totalElements ?? 0,
      sortBy: undefined,
      descending: true,
    })),
  }),
}));

vi.mock('axios', () => ({
  default: {
    isAxiosError: (err) => err?.isAxiosError === true,
  },
}));

vi.mock('src/services/AccountsService', () => ({
  getAccounts: vi.fn(),
}));

vi.mock('src/mappers/accountMapper', () => ({
  useAccountMapper: () => ({
    toAccountQueryFilterDTO: vi.fn((filters) => filters),
    toAccountList: vi.fn((list) => list),
  }),
}));

vi.mock('src/composables/AccountsColumns', () => ({
  useAccountsColumns: vi.fn(() => []),
}));

vi.mock('assets/accounts/AccountsFilters', () => ({
  fieldsSearch: [],
  defaultFields: [],
  advancedFields: [],
}));

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter,
}));

const mockPage = (items = [], total = 0) => ({
  content: items,
  totalElements: total,
});

describe('Test component: Accounts', () => {
  let wrapper;

  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetAccounts.mockResolvedValue(mockPage());
    wrapper = shallowMount(AccountsPage);
  });

  describe('Test function: loadData', () => {
    it('should call getAccounts and populate accounts', async () => {
      const accounts = [
        { id: '1', firstname: 'John', lastname: 'Doe', email: 'j@d.com' },
        { id: '2', firstname: 'Jane', lastname: 'Smith', email: 'j@s.com' },
      ];
      mockedGetAccounts.mockResolvedValue(mockPage(accounts, 2));

      await wrapper.vm.loadData();

      expect(getAccounts).toHaveBeenCalled();
      expect(wrapper.vm.accounts).toEqual(accounts);
      expect(wrapper.vm.pagination.rowsNumber).toBe(2);
    });

    it('should set isLoading to true during data load and false after', async () => {
      await flushPromises();

      expect(wrapper.vm.isLoading).toBe(false);

      const loadPromise = wrapper.vm.loadData();
      expect(wrapper.vm.isLoading).toBe(true);

      await loadPromise;
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify with notFound message when API returns 404', async () => {
      mockedGetAccounts.mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 404 },
      });

      await wrapper.vm.loadData();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.notFound',
      });
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify with generic message when a non-404 error occurs', async () => {
      mockedGetAccounts.mockRejectedValueOnce(new Error('boom'));

      await wrapper.vm.loadData();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.generic',
      });
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should use current pagination when calling getAccounts', async () => {
      wrapper.vm.pagination.page = 2;
      wrapper.vm.pagination.rowsPerPage = 25;
      await wrapper.vm.loadData();

      expect(getAccounts).toHaveBeenLastCalledWith(
        expect.anything(),
        expect.objectContaining({ page: 1, size: 25 })
      );
    });
  });

  describe('Test function: goToCreate', () => {
    it('should navigate to creation page', () => {
      wrapper.vm.goToCreate();
      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/accounts/create',
      });
    });
  });

  describe('Test function: goToAccountDetails', () => {
    it('should navigate to the account detail page', () => {
      wrapper.vm.goToAccountDetails({ id: 'abc-123' });
      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/accounts/abc-123',
      });
    });
  });

  describe('Test function: onRequest', () => {
    it('should update pagination and reload data', async () => {
      const newPagination = {
        page: 3,
        rowsPerPage: 20,
        sortBy: 'lastname',
        descending: false,
        rowsNumber: 100,
      };

      await wrapper.vm.onRequest({ pagination: newPagination });

      expect(getAccounts).toHaveBeenCalledTimes(2); // onMounted + onRequest
      expect(getAccounts).toHaveBeenLastCalledWith(
        expect.anything(),
        expect.objectContaining({
          page: 2,
          size: 20,
          sort: 'lastname',
          direction: 'asc',
        })
      );
    });
  });

  describe('Test function: onFiltersChange', () => {
    it('should reset page to 1, update filters and reload data', async () => {
      wrapper.vm.pagination.page = 5;
      const newFilters = { firstname: 'Alice' };
      await wrapper.vm.onFiltersChange(newFilters);

      expect(wrapper.vm.filters).toEqual(newFilters);
      expect(wrapper.vm.pagination.page).toBe(1);
      expect(getAccounts).toHaveBeenCalledTimes(2); // onMounted + onFiltersChange
    });
  });

  describe('Test hook: onMounted', () => {
    it('should call loadData on mount', async () => {
      await flushPromises();
      expect(getAccounts).toHaveBeenCalledTimes(1);
    });
  });
});
