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
import { createAccount } from 'src/services/AccountService';
import {
  getOrganizationalUnitById,
  getOrganizationalUnitRoot,
} from 'src/services/OrganizationalUnitService';
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';
import AccountCreationPage from '../../../src/pages/AccountCreationPage.vue';

const mockedCreateAccount = vi.mocked(createAccount);
const mockedGetOrganizationalUnitRoot = vi.mocked(getOrganizationalUnitRoot);
const mockedGetOrganizationalUnitById = vi.mocked(getOrganizationalUnitById);
const mockNotify = vi.fn();

const mockRouter = {
  push: vi.fn(),
};

const mockRoute = {
  query: {},
};

vi.mock('@linagora/linid-im-front-corelib', () => ({
  loadAsyncComponent: vi.fn(() => null),
  useNotify: () => ({
    Notify: mockNotify,
  }),
  useScopedI18n: () => ({
    t: vi.fn((v) => v),
  }),
  useUiDesign: () => ({
    ui: vi.fn(() => ({})),
  }),
  usePagination: () => ({
    toPagination: vi.fn((p) => p),
  }),
}));

vi.mock('src/boot/config', () => ({
  appConfig: {
    accountCreationFields: [
      {
        name: 'organizationalUnit',
        type: 'String',
        input: 'Text',
        required: true,
      },
      { name: 'externalId', type: 'String', input: 'Text', required: true },
      { name: 'lastname', type: 'String', input: 'Text', required: true },
      { name: 'firstname', type: 'String', input: 'Text', required: true },
      { name: 'email', type: 'String', input: 'Email', required: true },
      {
        name: 'validityPeriodStart',
        type: 'String',
        input: 'Date',
        required: true,
      },
    ],
  },
}));

vi.mock('src/composables/useAccountMapper', () => ({
  useAccountMapper: () => ({
    toAccountRecord: (form) => ({
      externalId: form.externalId,
      lastname: form.lastname,
      firstname: form.firstname,
      email: form.email,
      validityPeriod: {
        start: form.validityPeriodStart,
        end: null,
      },
      organizationalUnit: form.organizationalUnit,
    }),
  }),
}));

vi.mock('src/composables/useCommonMapper', () => ({
  useCommonMapper: () => ({
    toEmptyRecord: () => ({
      externalId: '',
      lastname: '',
      firstname: '',
      email: '',
      validityPeriodStart: '',
      organizationalUnit: undefined,
    }),
  }),
}));

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: vi.fn((v) => v),
  }),
}));

vi.mock('axios', () => ({
  default: {
    isAxiosError: (err) => err?.isAxiosError === true,
  },
}));

vi.mock('src/services/AccountService', () => ({
  createAccount: vi.fn(),
}));

vi.mock('src/services/OrganizationalUnitService', () => ({
  getOrganizationalUnits: vi.fn(),
  getOrganizationalUnitRoot: vi.fn(),
  getOrganizationalUnitById: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter,
  useRoute: () => {
    // Return the current state of mockRoute to allow test modifications
    return mockRoute;
  },
}));

const createdAccount = {
  id: '11111111-1111-4111-8111-111111111111',
  externalId: 'external-id',
  firstname: 'John',
  lastname: 'Doe',
  email: 'john.doe@example.com',
  createdBy: 'creator-id',
  updatedBy: 'creator-id',
  insertDate: '2026-04-15T12:00:00.000000Z',
  updateDate: '2026-04-15T12:00:00.000000Z',
};

const rootOrganizationalUnit = {
  id: '22222222-2222-4222-8222-222222222222',
  name: 'root',
  type: 'ORG_UNIT',
};

const selectedOrganizationalUnit = {
  id: '33333333-3333-4333-8333-333333333333',
  name: 'Engineering',
  type: 'ORG_UNIT',
};

describe('Test component: AccountCreationPage', () => {
  let wrapper;

  beforeEach(() => {
    vi.clearAllMocks();
    mockRouter.push.mockClear();
    mockNotify.mockClear();
    mockRoute.query = {};
    mockedCreateAccount.mockResolvedValue(createdAccount);
    mockedGetOrganizationalUnitRoot.mockResolvedValue(rootOrganizationalUnit);
    mockedGetOrganizationalUnitById.mockResolvedValue(
      selectedOrganizationalUnit
    );
  });

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
  });

  describe('Test function: loadOrganizationalUnit', () => {
    it('should load root organizational unit when ouSelected is undefined', async () => {
      mockRoute.query = {};
      wrapper = shallowMount(AccountCreationPage);

      await flushPromises();
      await wrapper.vm.loadOrganizationalUnit();

      expect(mockedGetOrganizationalUnitRoot).toHaveBeenCalled();
      expect(wrapper.vm.organizationalUnit).toEqual(rootOrganizationalUnit);
    });

    it('should notify with notFound message when failing to fetch root OU with 404 error', async () => {
      mockRoute.query = {};
      wrapper = shallowMount(AccountCreationPage);
      await flushPromises();

      mockNotify.mockClear();
      mockedGetOrganizationalUnitRoot.mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 404 },
      });

      await wrapper.vm.loadOrganizationalUnit().catch(() => {
        // Error is expected and already handled
      });

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.notFound',
      });
      expect(mockRouter.push).toHaveBeenCalledWith('/accounts');
    });

    it('should notify with validation error when failing to fetch root OU with 400 error', async () => {
      mockRoute.query = {};
      wrapper = shallowMount(AccountCreationPage);
      await flushPromises();

      mockNotify.mockClear();
      mockedGetOrganizationalUnitRoot.mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 400 },
      });

      await wrapper.vm.loadOrganizationalUnit().catch(() => {
        // Error is expected and already handled
      });

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.generic',
      });
      expect(mockRouter.push).toHaveBeenCalledWith('/accounts');
    });

    it('should load specific organizational unit when ouSelected is provided', async () => {
      mockRoute.query = { ou: selectedOrganizationalUnit.id };
      wrapper = shallowMount(AccountCreationPage);
      await flushPromises();
      await wrapper.vm.loadOrganizationalUnit();

      expect(mockedGetOrganizationalUnitById).toHaveBeenCalledWith(
        selectedOrganizationalUnit.id
      );
      expect(wrapper.vm.organizationalUnit).toEqual(selectedOrganizationalUnit);
    });

    it('should notify and redirect when failing to fetch selected OU', async () => {
      mockRoute.query = { ou: selectedOrganizationalUnit.id };
      wrapper = shallowMount(AccountCreationPage);
      await flushPromises();

      mockedGetOrganizationalUnitById.mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 404 },
      });

      await wrapper.vm.loadOrganizationalUnit().catch(() => {
        // Error is expected and already handled
      });

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.notFound',
      });
      expect(mockRouter.push).toHaveBeenCalledWith('/accounts');
    });

    it('should notify with generic error when failing to fetch selected OU', async () => {
      mockRoute.query = { ou: selectedOrganizationalUnit.id };
      wrapper = shallowMount(AccountCreationPage);
      await flushPromises();

      mockedGetOrganizationalUnitById.mockRejectedValueOnce(
        new Error('Network error')
      );

      await wrapper.vm.loadOrganizationalUnit().catch(() => {
        // Error is expected and already handled
      });

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.generic',
      });
      expect(mockRouter.push).toHaveBeenCalledWith('/accounts');
    });
  });

  describe('Test function: setFormData', () => {
    it('should set the organizational unit name in the form', async () => {
      wrapper = shallowMount(AccountCreationPage);
      await flushPromises();
      wrapper.vm.organizationalUnit = rootOrganizationalUnit;

      await wrapper.vm.setFormData();

      expect(wrapper.vm.form.organizationalUnit).toEqual(
        rootOrganizationalUnit.name
      );
    });
  });

  describe('Test function: onSubmit', () => {
    it('should call createAccount with the current form data including organizationalUnit', async () => {
      wrapper = shallowMount(AccountCreationPage);
      await flushPromises();
      wrapper.vm.organizationalUnit = selectedOrganizationalUnit;
      wrapper.vm.form.externalId = 'external-id';
      wrapper.vm.form.lastname = 'Doe';
      wrapper.vm.form.firstname = 'John';
      wrapper.vm.form.email = 'john.doe@example.com';
      wrapper.vm.form.validityPeriodStart = '2026-05-01';
      wrapper.vm.form.organizationalUnit = selectedOrganizationalUnit.id;

      await wrapper.vm.onSubmit();

      expect(createAccount).toHaveBeenCalledWith({
        externalId: 'external-id',
        lastname: 'Doe',
        firstname: 'John',
        email: 'john.doe@example.com',
        validityPeriod: {
          start: '2026-05-01',
          end: null,
        },
        organizationalUnit: selectedOrganizationalUnit.id,
      });
    });

    it('should notify the user and redirect to the created account on success', async () => {
      wrapper = shallowMount(AccountCreationPage);
      await flushPromises();
      wrapper.vm.organizationalUnit = selectedOrganizationalUnit;
      wrapper.vm.form.externalId = 'external-id';
      wrapper.vm.form.lastname = 'Doe';
      wrapper.vm.form.firstname = 'John';
      wrapper.vm.form.email = 'john.doe@example.com';
      wrapper.vm.form.organizationalUnit = selectedOrganizationalUnit.id;

      await wrapper.vm.onSubmit();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'positive',
        message: 'success',
      });
      expect(mockRouter.push).toHaveBeenCalledWith(
        `/accounts/${createdAccount.id}`
      );
    });

    it('should toggle isLoading around the request', async () => {
      wrapper = shallowMount(AccountCreationPage);
      await flushPromises();
      wrapper.vm.organizationalUnit = selectedOrganizationalUnit;
      wrapper.vm.form.externalId = 'external-id';
      wrapper.vm.form.lastname = 'Doe';
      wrapper.vm.form.firstname = 'John';
      wrapper.vm.form.email = 'john.doe@example.com';
      wrapper.vm.form.organizationalUnit = selectedOrganizationalUnit.id;

      const submitPromise = wrapper.vm.onSubmit();
      expect(wrapper.vm.isLoading).toBe(true);

      await submitPromise;
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify with validation key and stay on page when backend returns 400', async () => {
      mockedCreateAccount.mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 400 },
      });

      wrapper = shallowMount(AccountCreationPage);
      await flushPromises();
      mockRouter.push.mockClear();
      wrapper.vm.organizationalUnit = selectedOrganizationalUnit;
      wrapper.vm.form.organizationalUnit = selectedOrganizationalUnit.id;

      await wrapper.vm.onSubmit();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.validation',
      });
      expect(mockRouter.push).not.toHaveBeenCalled();
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify with generic key when a non-400 error occurs', async () => {
      mockedCreateAccount.mockRejectedValueOnce(new Error('boom'));

      wrapper = shallowMount(AccountCreationPage);
      await flushPromises();
      mockRouter.push.mockClear();
      wrapper.vm.organizationalUnit = selectedOrganizationalUnit;
      wrapper.vm.form.organizationalUnit = selectedOrganizationalUnit.id;

      await wrapper.vm.onSubmit();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.generic',
      });
      expect(mockRouter.push).not.toHaveBeenCalled();
      expect(wrapper.vm.isLoading).toBe(false);
    });
  });

  describe('Test function: cancel', () => {
    it('should navigate back to the accounts list', async () => {
      mockRoute.query = {};
      wrapper = shallowMount(AccountCreationPage);
      await flushPromises();
      wrapper.vm.cancel();

      // The cancel function creates a query object with node: '', then updates it if needed
      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/accounts',
        query: { node: '' },
      });
    });

    it('should include node parameter when navigating back if it was in the query', async () => {
      mockRoute.query = { node: 'some-node-id' };
      wrapper = shallowMount(AccountCreationPage);
      await flushPromises();
      wrapper.vm.cancel();

      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/accounts',
        query: { node: 'some-node-id' },
      });
    });
  });
});
