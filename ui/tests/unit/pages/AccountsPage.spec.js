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
import { getAccountsByOrganizationalUnitId } from 'src/services/OrganizationalUnitService';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick, ref } from 'vue';
import AccountsPage from '../../../src/pages/AccountsPage.vue';

const mockedGetAccountsByOrganizationalUnitId = vi.mocked(
  getAccountsByOrganizationalUnitId
);

const mockSelectedOrganizationalUnitId = ref('');

const mockRouter = {
  push: vi.fn(),
};

const mockRoute = {
  path: '/accounts',
  query: {},
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
    isCancel: (err) => err?.isCanceled === true,
  },
}));

vi.mock('src/services/OrganizationalUnitService', () => ({
  getAccountsByOrganizationalUnitId: vi.fn(),
}));

vi.mock('src/stores/useOrganizationalUnitStore', () => ({
  useOrganizationalUnitStore: () => ({
    selectedOrganizationalUnitId: mockSelectedOrganizationalUnitId,
  }),
}));

vi.mock('pinia', () => ({
  storeToRefs: (store) => store,
  defineStore: vi.fn(),
}));

vi.mock('src/composables/useAccountMapper', () => ({
  useAccountMapper: () => ({
    toAccountQueryFilterDTO: vi.fn((filters) => filters),
    toAccountList: vi.fn((list) => list),
  }),
}));

vi.mock('src/composables/useAccountsColumns', () => ({
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

const mockPage = (items = [], total = 0, number = 0, size = 10) => ({
  content: items,
  totalElements: total,
  number,
  size,
});

describe('Test component: AccountsPage', () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  beforeEach(() => {
    vi.clearAllMocks();
    mockSelectedOrganizationalUnitId.value = '';
    mockedGetAccountsByOrganizationalUnitId.mockResolvedValue(mockPage());
    wrapper = shallowMount(AccountsPage);
  });

  describe('Test function: loadData', () => {
    it('should call getAccountsByOrganizationalUnitId with the given id and populate accounts', async () => {
      const accounts = [
        { id: '1', firstname: 'John', lastname: 'Doe', email: 'j@d.com' },
        { id: '2', firstname: 'Jane', lastname: 'Smith', email: 'j@s.com' },
      ];
      mockedGetAccountsByOrganizationalUnitId.mockResolvedValue(
        mockPage(accounts, 2)
      );

      await wrapper.vm.loadData('ou-uuid');

      expect(getAccountsByOrganizationalUnitId).toHaveBeenCalledWith(
        'ou-uuid',
        expect.anything(),
        expect.anything(),
        expect.any(AbortSignal)
      );
      expect(wrapper.vm.accounts).toEqual(accounts);
      expect(wrapper.vm.pagination.rowsNumber).toBe(2);
    });

    it('should set isLoading to true during data load and false after', async () => {
      const loadPromise = wrapper.vm.loadData('ou-uuid');
      expect(wrapper.vm.isLoading).toBe(true);

      await loadPromise;
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should set isLoading to false even when an error occurs', async () => {
      mockedGetAccountsByOrganizationalUnitId.mockRejectedValueOnce(
        new Error('boom')
      );

      await wrapper.vm.loadData('ou-uuid');

      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify with notFound message when API returns 404', async () => {
      mockedGetAccountsByOrganizationalUnitId.mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 404 },
      });

      await wrapper.vm.loadData('ou-uuid');

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.notFound',
      });
    });

    it('should notify with generic message when a non-404 error occurs', async () => {
      mockedGetAccountsByOrganizationalUnitId.mockRejectedValueOnce(
        new Error('boom')
      );

      await wrapper.vm.loadData('ou-uuid');

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.generic',
      });
    });

    it('should abort the previous request when a new load starts', async () => {
      let firstSignal;
      mockedGetAccountsByOrganizationalUnitId.mockImplementationOnce(
        (id, filters, pagination, signal) => {
          firstSignal = signal;
          return new Promise(() => {});
        }
      );
      mockedGetAccountsByOrganizationalUnitId.mockResolvedValueOnce(mockPage());

      void wrapper.vm.loadData('ou-uuid');
      await wrapper.vm.loadData('second-ou-uuid');

      expect(firstSignal.aborted).toBe(true);
    });

    it('should not notify when the request is canceled', async () => {
      mockedGetAccountsByOrganizationalUnitId.mockRejectedValueOnce({
        isCanceled: true,
      });

      await wrapper.vm.loadData('ou-uuid');

      expect(mockNotify).not.toHaveBeenCalled();
    });

    it('should pass current pagination to getAccountsByOrganizationalUnitId', async () => {
      wrapper.vm.pagination.page = 2;
      wrapper.vm.pagination.rowsPerPage = 25;

      await wrapper.vm.loadData('ou-uuid');

      expect(getAccountsByOrganizationalUnitId).toHaveBeenLastCalledWith(
        'ou-uuid',
        expect.anything(),
        expect.objectContaining({ page: 1, size: 25 }),
        expect.any(AbortSignal)
      );
    });
  });

  describe('Test watcher: selectedOrganizationalUnitId', () => {
    it('should call loadData with the new OU id when selectedOrganizationalUnitId changes', async () => {
      mockSelectedOrganizationalUnitId.value = 'new-ou-uuid';
      await nextTick();
      await flushPromises();

      expect(getAccountsByOrganizationalUnitId).toHaveBeenCalledWith(
        'new-ou-uuid',
        expect.anything(),
        expect.anything(),
        expect.any(AbortSignal)
      );
    });

    it('should call loadData again with the latest OU id on each change', async () => {
      mockSelectedOrganizationalUnitId.value = 'first-ou-uuid';
      await nextTick();
      await flushPromises();

      mockSelectedOrganizationalUnitId.value = 'second-ou-uuid';
      await nextTick();
      await flushPromises();

      expect(getAccountsByOrganizationalUnitId).toHaveBeenLastCalledWith(
        'second-ou-uuid',
        expect.anything(),
        expect.anything(),
        expect.any(AbortSignal)
      );
    });

    it('should not call loadData on mount when selectedOrganizationalUnitId is empty', async () => {
      await flushPromises();

      expect(getAccountsByOrganizationalUnitId).not.toHaveBeenCalled();
    });
  });

  describe('Test function: goToCreate', () => {
    it('should navigate to the creation page with selected organizational unit and node', () => {
      mockSelectedOrganizationalUnitId.value = 'ou-uuid';
      mockRoute.query = { node: 'node-123' };
      wrapper.vm.goToCreate();

      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/accounts/create',
        query: {
          ou: 'ou-uuid',
          node: 'node-123',
        },
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
    it('should update pagination and reload data using the current selectedOrganizationalUnitId', async () => {
      mockSelectedOrganizationalUnitId.value = 'ou-uuid';
      await nextTick();
      await flushPromises();

      const newPagination = {
        page: 3,
        rowsPerPage: 20,
        sortBy: 'lastname',
        descending: false,
        rowsNumber: 100,
      };

      await wrapper.vm.onRequest({ pagination: newPagination });

      expect(getAccountsByOrganizationalUnitId).toHaveBeenLastCalledWith(
        'ou-uuid',
        expect.anything(),
        expect.objectContaining({
          page: 2,
          size: 20,
          sort: 'lastname',
          direction: 'asc',
        }),
        expect.any(AbortSignal)
      );
    });
  });

  describe('Test function: onFiltersChange', () => {
    it('should reset page to 1, update filters and reload data with the current OU id', async () => {
      mockSelectedOrganizationalUnitId.value = 'ou-uuid';
      await nextTick();
      await flushPromises();

      wrapper.vm.pagination.page = 5;
      const newFilters = { firstname: 'Alice' };

      await wrapper.vm.onFiltersChange(newFilters);

      expect(wrapper.vm.filters).toEqual(newFilters);
      expect(wrapper.vm.pagination.page).toBe(1);
      expect(getAccountsByOrganizationalUnitId).toHaveBeenLastCalledWith(
        'ou-uuid',
        expect.objectContaining({ firstname: 'Alice' }),
        expect.anything(),
        expect.any(AbortSignal)
      );
    });
  });
});
