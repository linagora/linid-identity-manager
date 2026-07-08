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
import { getApplicationById } from 'src/services/ApplicationService';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ApplicationDetailsPage from '../../../src/pages/ApplicationDetailsPage.vue';

const mockedGetApplicationById = vi.mocked(getApplicationById);

const { mockNotify, mockScopedT } = vi.hoisted(() => ({
  mockNotify: vi.fn(),
  mockScopedT: vi.fn((v) => v),
}));

const mockRoute = {
  params: {
    id: 'test-application-id',
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
    t: mockScopedT,
  }),
}));

vi.mock('axios', () => ({
  default: {
    isAxiosError: (err) => err?.isAxiosError === true,
  },
}));

vi.mock('src/services/ApplicationService', () => ({
  getApplicationById: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter,
}));

describe('Test component: ApplicationDetailsPage', () => {
  let wrapper;

  beforeEach(() => {
    vi.clearAllMocks();
    mockRoute.params.id = 'test-application-id';
    mockedGetApplicationById.mockResolvedValue({
      id: 'test-application-id',
      code: 'OPAL',
      name: 'OPAL',
      description: 'Management tool',
      type: 'OIDC',
      claimsTemplate: '|Id|Nom',
      deployedAt: null,
      configuration: '{}',
      roles: [],
      createdBy: 'Alice Creator',
      updatedBy: 'Bob Updater',
      insertDate: '2026-04-15T12:00:00.000000Z',
      updateDate: '2026-04-16T09:30:00.000000Z',
    });
  });

  describe('Test computed: applicationId', () => {
    it('should retrieve valid application id from route params', () => {
      wrapper = shallowMount(ApplicationDetailsPage);

      expect(wrapper.vm.applicationId).toBe('test-application-id');
    });
  });

  describe('Test function: loadApplication', () => {
    it('should retrieve application data and expose it', async () => {
      wrapper = shallowMount(ApplicationDetailsPage);

      await wrapper.vm.loadApplication();

      expect(getApplicationById).toHaveBeenCalledWith('test-application-id');
      expect(wrapper.vm.application).toMatchObject({
        id: 'test-application-id',
        code: 'OPAL',
        name: 'OPAL',
        description: 'Management tool',
      });
    });

    it('should set isLoading to true during data load and false after', async () => {
      wrapper = shallowMount(ApplicationDetailsPage);
      await flushPromises();

      expect(wrapper.vm.isLoading).toBe(false);

      const loadPromise = wrapper.vm.loadApplication();
      expect(wrapper.vm.isLoading).toBe(true);

      await loadPromise;
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify and redirect with notFound message when API returns 404', async () => {
      mockedGetApplicationById.mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 404 },
      });

      wrapper = shallowMount(ApplicationDetailsPage);
      await wrapper.vm.loadApplication();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.notFound',
      });
      expect(mockRouter.push).toHaveBeenCalledWith('/applications');
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify with generic message and redirect when a non-404 error occurs', async () => {
      mockedGetApplicationById.mockRejectedValueOnce(new Error('boom'));

      wrapper = shallowMount(ApplicationDetailsPage);
      await wrapper.vm.loadApplication();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.generic',
      });
      expect(mockRouter.push).toHaveBeenCalledWith('/applications');
      expect(wrapper.vm.isLoading).toBe(false);
    });
  });

  describe('Test function: goBack', () => {
    it('should navigate to applications list', () => {
      wrapper = shallowMount(ApplicationDetailsPage);
      wrapper.vm.goBack();

      expect(mockRouter.push).toHaveBeenCalledWith('/applications');
    });
  });

  describe('Test hook: onMounted', () => {
    it('should call loadApplication on mount', async () => {
      wrapper = shallowMount(ApplicationDetailsPage);
      await flushPromises();

      expect(getApplicationById).toHaveBeenCalledWith('test-application-id');
    });
  });
});
