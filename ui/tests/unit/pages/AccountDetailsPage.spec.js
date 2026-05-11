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
import { getAccountById } from 'src/services/AccountService';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AccountDetailsPage from '../../../src/pages/AccountDetailsPage.vue';

const mockedGetAccountById = vi.mocked(getAccountById);
const mockNotify = vi.fn();

const mockRoute = {
  params: {
    id: 'test-account-id',
  },
};

const mockRouter = {
  push: vi.fn(),
};

vi.mock('@linagora/linid-im-front-corelib', () => ({
  loadAsyncComponent: vi.fn(() => null),
  useNotify: () => ({
    Notify: mockNotify,
  }),
  useScopedI18n: () => ({
    t: vi.fn((v) => v),
  }),
}));

vi.mock('axios', () => ({
  default: {
    isAxiosError: (err) => err?.isAxiosError === true,
  },
}));

vi.mock('src/services/AccountService', () => ({
  getAccountById: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter,
}));

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: vi.fn((v) => v) }),
}));

describe('Test component: AccountDetailsPage', () => {
  let wrapper;

  beforeEach(() => {
    vi.clearAllMocks();
    mockRoute.params.id = 'test-account-id';
    mockedGetAccountById.mockResolvedValue({
      id: 'test-account-id',
      firstname: 'John',
      lastname: 'Doe',
      email: 'john.doe@example.com',
      createdBy: 'Alice Creator',
      updatedBy: 'Bob Updater',
      insertDate: '2026-04-15T12:00:24.814930Z',
      updateDate: '2026-04-16T09:30:00.000000Z',
      status: 'ACTIVE',
      validityPeriod: { start: '2026-01-01T00:00:00Z', end: null },
      suspensionPeriod: { start: null, end: null },
      activationAt: '2026-01-01T00:00:00Z',
      statusReason: null,
      statusSubreason: null,
      statusComment: null,
      daysBeforeDeactivation: null,
    });
  });

  describe('Test computed: accountId', () => {
    it('should retrieve valid account id from route params', () => {
      wrapper = shallowMount(AccountDetailsPage);

      expect(wrapper.vm.accountId).toBe('test-account-id');
    });
  });

  describe('Test function: loadAccount', () => {
    it('should retrieve account data and split it between account and accountStatus', async () => {
      wrapper = shallowMount(AccountDetailsPage);

      await wrapper.vm.loadAccount();

      expect(getAccountById).toHaveBeenCalledWith('test-account-id');
      expect(wrapper.vm.account).toMatchObject({
        id: 'test-account-id',
        firstname: 'John',
        lastname: 'Doe',
        email: 'john.doe@example.com',
      });
      expect(wrapper.vm.accountStatus).toMatchObject({
        status: 'ACTIVE',
        validityPeriod: { start: '2026-01-01T00:00:00Z', end: null },
        suspensionPeriod: { start: null, end: null },
        activationAt: '2026-01-01T00:00:00Z',
      });
      expect(wrapper.vm.accountStatus).not.toHaveProperty('firstname');
    });

    it('should set isLoading to true during data load and false after', async () => {
      wrapper = shallowMount(AccountDetailsPage);
      await flushPromises();

      expect(wrapper.vm.isLoading).toBe(false);

      const loadPromise = wrapper.vm.loadAccount();
      expect(wrapper.vm.isLoading).toBe(true);

      await loadPromise;
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify and redirect with notFound message when API returns 404', async () => {
      mockedGetAccountById.mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 404 },
      });

      wrapper = shallowMount(AccountDetailsPage);
      await wrapper.vm.loadAccount();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.notFound',
      });
      expect(mockRouter.push).toHaveBeenCalledWith('/accounts');
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify with generic message and redirect when a non-404 error occurs', async () => {
      mockedGetAccountById.mockRejectedValueOnce(new Error('boom'));

      wrapper = shallowMount(AccountDetailsPage);
      await wrapper.vm.loadAccount();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.generic',
      });
      expect(mockRouter.push).toHaveBeenCalledWith('/accounts');
      expect(wrapper.vm.isLoading).toBe(false);
    });
  });

  describe('Test function: goBack', () => {
    it('should navigate to accounts list', () => {
      wrapper = shallowMount(AccountDetailsPage);
      wrapper.vm.goBack();

      expect(mockRouter.push).toHaveBeenCalledWith('/accounts');
    });
  });

  describe('Test hook: onMounted', () => {
    it('should call loadAccount on mount', async () => {
      wrapper = shallowMount(AccountDetailsPage);
      await flushPromises();

      expect(getAccountById).toHaveBeenCalledWith('test-account-id');
    });
  });

  describe('Test computed: lifecycleUi', () => {
    it('should be null when no account is loaded', () => {
      wrapper = shallowMount(AccountDetailsPage);
      expect(wrapper.vm.lifecycleUi).toBeNull();
    });

    it('should expose the projected lifecycle UI once the account is loaded', async () => {
      wrapper = shallowMount(AccountDetailsPage);
      await wrapper.vm.loadAccount();

      const ui = wrapper.vm.lifecycleUi;
      expect(ui).not.toBeNull();
      expect(ui.showBadge).toBe(true);
      expect(ui.suspensionMenuItems).toEqual([
        { key: 'suspension.immediate', clickable: true },
        { key: 'suspension.scheduled', clickable: true },
      ]);
      expect(ui.deactivationMenuItems).toEqual([
        { key: 'deactivation.immediate', clickable: true },
        { key: 'deactivation.scheduled', clickable: true },
      ]);
    });
  });

  describe('Test computed: hasAnyLifecycleAction', () => {
    it('should be false when no account is loaded', () => {
      wrapper = shallowMount(AccountDetailsPage);
      expect(wrapper.vm.hasAnyLifecycleAction).toBe(false);
    });

    it('should be true when at least one family exposes menu items', async () => {
      wrapper = shallowMount(AccountDetailsPage);
      await wrapper.vm.loadAccount();
      expect(wrapper.vm.hasAnyLifecycleAction).toBe(true);
    });
  });
});
