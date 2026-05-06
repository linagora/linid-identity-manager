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

import { shallowMount } from '@vue/test-utils';
import { createAccount } from 'src/services/AccountService';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AccountCreationPage from '../../../src/pages/AccountCreationPage.vue';

const mockedCreateAccount = vi.mocked(createAccount);
const mockNotify = vi.fn();

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
  useUiDesign: () => ({
    ui: vi.fn(() => ({})),
  }),
}));

vi.mock('src/composables/useAccountCreationConfig', () => ({
  useAccountCreationConfig: () => ({
    creationFields: [
      { name: 'externalId', label: 'External ID', type: 'text', rules: [] },
      { name: 'lastname', label: 'Last name', type: 'text', rules: [] },
      { name: 'firstname', label: 'First name', type: 'text', rules: [] },
      { name: 'email', label: 'Email', type: 'email', rules: [] },
    ],
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

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter,
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

const fillForm = (wrapper) => {
  wrapper.vm.form.externalId = 'external-id';
  wrapper.vm.form.lastname = 'Doe';
  wrapper.vm.form.firstname = 'John';
  wrapper.vm.form.email = 'john.doe@example.com';
};

describe('Test component: AccountCreationPage', () => {
  let wrapper;

  beforeEach(() => {
    vi.clearAllMocks();
    mockedCreateAccount.mockResolvedValue(createdAccount);
  });

  describe('Test function: onSubmit', () => {
    it('should call createAccount with the current form data', async () => {
      wrapper = shallowMount(AccountCreationPage);
      fillForm(wrapper);

      await wrapper.vm.onSubmit();

      expect(createAccount).toHaveBeenCalledWith({
        externalId: 'external-id',
        lastname: 'Doe',
        firstname: 'John',
        email: 'john.doe@example.com',
        validityPeriod: {
          start: null,
          end: null,
        },
      });
    });

    it('should notify the user and redirect to the created account on success', async () => {
      wrapper = shallowMount(AccountCreationPage);
      fillForm(wrapper);

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
    it('should navigate back to the accounts list', () => {
      wrapper = shallowMount(AccountCreationPage);
      wrapper.vm.cancel();

      expect(mockRouter.push).toHaveBeenCalledWith('/accounts');
    });
  });
});
