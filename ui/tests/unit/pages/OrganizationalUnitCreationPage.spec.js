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
import {
  createOrganizationalUnit,
  getOrganizationalUnitById,
} from 'src/services/OrganizationalUnitService';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OrganizationalUnitCreationPage from '../../../src/pages/OrganizationalUnitCreationPage.vue';

const mockedCreateOu = vi.mocked(createOrganizationalUnit);
const mockedGetOuById = vi.mocked(getOrganizationalUnitById);
const mockNotify = vi.fn();

const mockRouter = {
  push: vi.fn(),
};

const route = {
  query: { parent: 'parent-uuid' },
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

vi.mock('src/boot/config', () => ({
  appConfig: {
    organizationalUnitCreationFields: [
      { name: 'name', type: 'String', input: 'Text', required: true },
      { name: 'type', type: 'String', input: 'List', required: true },
    ],
  },
}));

vi.mock('src/composables/useCommonMapper', () => ({
  useCommonMapper: () => ({
    toEmptyRecord: () => ({ name: '', type: '' }),
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

vi.mock('src/services/OrganizationalUnitService', () => ({
  createOrganizationalUnit: vi.fn(),
  getOrganizationalUnitById: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter,
  useRoute: () => route,
}));

const parentOu = {
  id: 'parent-uuid',
  name: 'Root',
  type: 'root',
  createdBy: 'admin',
  updatedBy: 'admin',
  insertDate: '2026-05-13T12:00:00.000000Z',
  updateDate: '2026-05-13T12:00:00.000000Z',
};

const createdOu = {
  id: '11111111-1111-4111-8111-111111111111',
  name: 'Engineering',
  type: 'DEPARTMENT',
  createdBy: 'admin',
  updatedBy: 'admin',
  insertDate: '2026-05-13T12:00:00.000000Z',
  updateDate: '2026-05-13T12:00:00.000000Z',
};

const fillForm = (wrapper) => {
  wrapper.vm.form.name = 'Engineering';
  wrapper.vm.form.type = 'DEPARTMENT';
};

describe('Test component: OrganizationalUnitCreationPage', () => {
  let wrapper;

  beforeEach(() => {
    vi.clearAllMocks();
    route.query = { parent: 'parent-uuid' };
    mockedGetOuById.mockResolvedValue(parentOu);
    mockedCreateOu.mockResolvedValue(createdOu);
  });

  describe('Test parent context loading', () => {
    it('should fetch the parent OU and expose its name', async () => {
      wrapper = shallowMount(OrganizationalUnitCreationPage);
      await flushPromises();

      expect(getOrganizationalUnitById).toHaveBeenCalledWith('parent-uuid');
      expect(wrapper.vm.parentId).toBe(parentOu.id);
      expect(wrapper.vm.parentName).toBe(parentOu.name);
    });

    it('should notify and redirect to home when the parent query is missing', async () => {
      route.query = {};

      wrapper = shallowMount(OrganizationalUnitCreationPage);
      await flushPromises();

      expect(getOrganizationalUnitById).not.toHaveBeenCalled();
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.missingParent',
      });
      expect(mockRouter.push).toHaveBeenCalledWith('/');
    });

    it('should notify and redirect to home when the parent OU cannot be loaded', async () => {
      mockedGetOuById.mockRejectedValueOnce(new Error('not-found'));

      wrapper = shallowMount(OrganizationalUnitCreationPage);
      await flushPromises();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.missingParent',
      });
      expect(mockRouter.push).toHaveBeenCalledWith('/');
    });
  });

  describe('Test function: onSubmit', () => {
    it('should call createOrganizationalUnit with the form data and resolved parent', async () => {
      wrapper = shallowMount(OrganizationalUnitCreationPage);
      await flushPromises();
      fillForm(wrapper);

      await wrapper.vm.onSubmit();

      expect(createOrganizationalUnit).toHaveBeenCalledWith({
        parent: 'parent-uuid',
        name: 'Engineering',
        type: 'DEPARTMENT',
      });
    });

    it('should notify the user and redirect to the parent OU on success', async () => {
      wrapper = shallowMount(OrganizationalUnitCreationPage);
      await flushPromises();
      fillForm(wrapper);

      await wrapper.vm.onSubmit();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'positive',
        message: 'success',
      });
      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/organizational-units',
        query: { node: parentOu.id },
      });
    });

    it('should toggle isLoading around the request', async () => {
      wrapper = shallowMount(OrganizationalUnitCreationPage);
      await flushPromises();

      const submitPromise = wrapper.vm.onSubmit();
      expect(wrapper.vm.isLoading).toBe(true);

      await submitPromise;
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify with validation key and stay on page when backend returns 400', async () => {
      mockedCreateOu.mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 400 },
      });

      wrapper = shallowMount(OrganizationalUnitCreationPage);
      await flushPromises();
      mockRouter.push.mockClear();

      await wrapper.vm.onSubmit();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.validation',
      });
      expect(mockRouter.push).not.toHaveBeenCalled();
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify with generic key when a non-400 error occurs', async () => {
      mockedCreateOu.mockRejectedValueOnce(new Error('boom'));

      wrapper = shallowMount(OrganizationalUnitCreationPage);
      await flushPromises();
      mockRouter.push.mockClear();

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
    it('should navigate back to the parent OU details', async () => {
      wrapper = shallowMount(OrganizationalUnitCreationPage);
      await flushPromises();
      mockRouter.push.mockClear();

      wrapper.vm.cancel();

      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/organizational-units',
        query: { node: parentOu.id },
      });
    });
  });
});
