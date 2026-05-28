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
  getOrganizationalUnitById,
  updateOrganizationalUnitStatus,
} from 'src/services/OrganizationalUnitService';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OrganizationalUnitDetailsPage from '../../../src/pages/OrganizationalUnitDetailsPage.vue';

const mockedGetById = vi.mocked(getOrganizationalUnitById);
const mockedUpdateStatus = vi.mocked(updateOrganizationalUnitStatus);
const mockNotify = vi.fn();
const mockUiEventNext = vi.fn();

const OU_ID = 'test-ou-id';

const mockRoute = {
  params: {
    id: OU_ID,
  },
};

const mockRouter = {
  push: vi.fn(),
};

vi.mock('@linagora/linid-im-front-corelib', () => ({
  loadAsyncComponent: vi.fn(() => null),
  uiEventSubject: { next: (...args) => mockUiEventNext(...args) },
  getI18nInstance: () => ({
    global: { t: vi.fn((v) => v) },
  }),
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

vi.mock('src/services/OrganizationalUnitService', () => ({
  getOrganizationalUnitById: vi.fn(),
  updateOrganizationalUnitStatus: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter,
}));

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: vi.fn((key) =>
      key === 'application.dateFormat' ? 'YYYY/MM/DD' : key
    ),
  }),
}));

const buildOuDto = (overrides = {}) => ({
  id: OU_ID,
  name: 'Engineering',
  type: 'DEPARTMENT',
  createdBy: 'Alice Creator',
  updatedBy: 'Bob Updater',
  insertDate: '2026-04-15T12:00:24.814930Z',
  updateDate: '2026-04-16T09:30:00.000000Z',
  suspensionPeriod: null,
  statusReason: null,
  statusSubreason: null,
  statusComment: null,
  isSuspended: false,
  parents: [],
  ...overrides,
});

describe('Test component: OrganizationalUnitDetailsPage', () => {
  let wrapper;

  beforeEach(() => {
    vi.clearAllMocks();
    mockRoute.params.id = OU_ID;
    mockedGetById.mockResolvedValue(buildOuDto());
    mockedUpdateStatus.mockResolvedValue(buildOuDto());
  });

  describe('Test computed: organizationalUnitId', () => {
    it('should retrieve valid OU id from route params', () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);

      expect(wrapper.vm.organizationalUnitId).toBe(OU_ID);
    });
  });

  describe('Test function: loadOrganizationalUnit', () => {
    it('should retrieve OU data and split it between identity and status', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);

      await wrapper.vm.loadOrganizationalUnit();

      expect(getOrganizationalUnitById).toHaveBeenCalledWith(OU_ID);
      expect(wrapper.vm.organizationalUnit).toMatchObject({
        id: OU_ID,
        name: 'Engineering',
        type: 'DEPARTMENT',
      });
      expect(wrapper.vm.organizationalUnitStatus).toMatchObject({
        isSuspended: false,
        suspensionPeriod: null,
      });
      expect(wrapper.vm.organizationalUnit).not.toHaveProperty('isSuspended');
      expect(wrapper.vm.organizationalUnitStatus).not.toHaveProperty('name');
    });

    it('should toggle isLoading around the API call', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await flushPromises();

      expect(wrapper.vm.isLoading).toBe(false);

      const loadPromise = wrapper.vm.loadOrganizationalUnit();
      expect(wrapper.vm.isLoading).toBe(true);

      await loadPromise;
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify and redirect home with notFound message on 404', async () => {
      mockedGetById.mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 404 },
      });

      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.notFound',
      });
      expect(mockRouter.push).toHaveBeenCalledWith('/');
    });

    it('should notify generic and redirect home on non-404 errors', async () => {
      mockedGetById.mockRejectedValueOnce(new Error('boom'));

      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.generic',
      });
      expect(mockRouter.push).toHaveBeenCalledWith('/');
    });
  });

  describe('Test function: goHome', () => {
    it('should navigate to the root path', () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      wrapper.vm.goHome();

      expect(mockRouter.push).toHaveBeenCalledWith('/');
    });
  });

  describe('Test hook: onMounted', () => {
    it('should call loadOrganizationalUnit on mount', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await flushPromises();

      expect(getOrganizationalUnitById).toHaveBeenCalledWith(OU_ID);
    });
  });

  describe('Test computed: lifecycleUi', () => {
    it('should be null while the OU is not loaded', () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      expect(wrapper.vm.lifecycleUi).toBeNull();
    });

    it('should project a not-suspended OU into the suspension dropdown UI state', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit();

      const ui = wrapper.vm.lifecycleUi;
      expect(ui.showBadge).toBe(true);
      expect(ui.showSuspendedBanner).toBeFalsy();
      expect(ui.suspensionMenuItems).toEqual([
        { key: 'suspension.immediate', clickable: true },
        { key: 'suspension.scheduled', clickable: true },
      ]);
      expect(ui.activationMenuItems).toBeUndefined();
    });

    it('should project a suspended OU into the reactivation UI state', async () => {
      mockedGetById.mockReset();
      mockedGetById.mockResolvedValue(
        buildOuDto({
          isSuspended: true,
          suspensionPeriod: { start: '2026-01-01T00:00:00Z', end: null },
        })
      );

      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit();

      const ui = wrapper.vm.lifecycleUi;
      expect(ui.showBadge).toBe(true);
      expect(ui.showSuspendedBanner).toBe(true);
      expect(ui.activationMenuItems).toEqual([
        { key: 'reactivation.immediate', clickable: true },
      ]);
      expect(ui.suspensionMenuItems).toBeUndefined();
    });
  });

  describe('Test computed: hasAnyLifecycleAction', () => {
    it('should be false while the OU is not loaded', () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      expect(wrapper.vm.hasAnyLifecycleAction).toBe(false);
    });

    it('should be true once at least one dropdown exposes menu items', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit();
      expect(wrapper.vm.hasAnyLifecycleAction).toBe(true);
    });
  });

  describe('Test function: onLifecycleActionClick', () => {
    beforeEach(async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit();
      mockUiEventNext.mockClear();
    });

    it('should open the immediate suspension confirmation dialog', () => {
      wrapper.vm.onLifecycleActionClick({ key: 'suspension.immediate' });

      expect(mockUiEventNext).toHaveBeenCalledWith(
        expect.objectContaining({
          key: 'confirmation',
          data: expect.objectContaining({
            i18nScope: 'OrganizationalUnitSuspendDialog',
            type: 'open',
          }),
        })
      );
    });

    it('should submit an immediate suspension starting in the future to avoid a past start', async () => {
      const fixedNow = new Date('2026-05-28T10:00:00.000Z');
      const fixedNowPlus1h = '2026-05-28T11:00:00.000Z';
      vi.useFakeTimers();
      vi.setSystemTime(fixedNow);

      try {
        wrapper.vm.onLifecycleActionClick({ key: 'suspension.immediate' });
        const { onConfirm } = mockUiEventNext.mock.calls[0][0].data;
        await onConfirm();

        expect(updateOrganizationalUnitStatus).toHaveBeenCalledWith(OU_ID, {
          suspensionPeriod: {
            start: fixedNowPlus1h,
            end: null,
          },
          reason: null,
          subreason: null,
          comment: null,
        });
      } finally {
        vi.useRealTimers();
      }
    });

    it('should open the schedule suspension form dialog with date, reason and comment fields', () => {
      wrapper.vm.onLifecycleActionClick({ key: 'suspension.scheduled' });

      const call = mockUiEventNext.mock.calls[0][0];
      expect(call.key).toBe('form');
      expect(call.data.i18nScope).toBe(
        'OrganizationalUnitScheduleSuspensionDialog'
      );
      expect(call.data.formFields.map((f) => f.name)).toEqual([
        'start',
        'end',
        'reason',
        'subreason',
        'comment',
      ]);
    });

    it('should open the reactivation confirmation dialog on reactivation.immediate', () => {
      wrapper.vm.onLifecycleActionClick({ key: 'reactivation.immediate' });

      expect(mockUiEventNext).toHaveBeenCalledWith(
        expect.objectContaining({
          key: 'confirmation',
          data: expect.objectContaining({
            i18nScope: 'OrganizationalUnitReactivateDialog',
          }),
        })
      );
    });

    it('should ignore unknown action keys', () => {
      wrapper.vm.onLifecycleActionClick({ key: 'unknown.action' });

      expect(mockUiEventNext).not.toHaveBeenCalled();
    });
  });

  describe('Test function: onModifySuspensionEnd', () => {
    it('should open the edit-suspension-end form dialog pre-filled with the current suspension', async () => {
      mockedGetById.mockReset();
      mockedGetById.mockResolvedValue(
        buildOuDto({
          isSuspended: true,
          suspensionPeriod: {
            start: '2026-01-01T00:00:00Z',
            end: '2026-12-31T00:00:00Z',
          },
          statusReason: 'Suspension Reason A',
          statusSubreason: 'Suspension Sub-reason A.1',
          statusComment: 'pre-filled',
        })
      );
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit();
      mockUiEventNext.mockClear();

      wrapper.vm.onModifySuspensionEnd();

      const call = mockUiEventNext.mock.calls[0][0];
      expect(call.key).toBe('form');
      expect(call.data.i18nScope).toBe(
        'OrganizationalUnitEditSuspensionEndDialog'
      );
      expect(call.data.initialFormData).toEqual({
        start: '2026/01/01',
        end: '2026/12/31',
        reason: 'Suspension Reason A',
        subreason: 'Suspension Sub-reason A.1',
        comment: 'pre-filled',
      });
    });

    it('should convert the localized end date to an ISO string before submitting', async () => {
      mockedGetById.mockReset();
      mockedGetById.mockResolvedValue(
        buildOuDto({
          isSuspended: true,
          suspensionPeriod: {
            start: '2026-01-01T00:00:00Z',
            end: '2026-12-31T00:00:00Z',
          },
        })
      );
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit();
      mockUiEventNext.mockClear();

      wrapper.vm.onModifySuspensionEnd();
      const { onSubmit } = mockUiEventNext.mock.calls[0][0].data;
      await onSubmit({ end: '2027/06/30' });

      expect(updateOrganizationalUnitStatus).toHaveBeenCalledWith(OU_ID, {
        suspensionPeriod: {
          start: '2026-01-01T00:00:00Z',
          end: '2027-06-30T00:00:00.000Z',
        },
        reason: null,
        subreason: null,
        comment: null,
      });
    });
  });

  describe('Test function: openScheduleSuspensionDialog', () => {
    it('should convert localized start and end dates to ISO strings before submitting', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit();
      mockUiEventNext.mockClear();

      wrapper.vm.openScheduleSuspensionDialog();
      const { onSubmit } = mockUiEventNext.mock.calls[0][0].data;
      await onSubmit({
        start: '2026/07/01',
        end: '2026/08/15',
        reason: 'INVESTIGATION',
        subreason: 'FRAUD',
        comment: 'scheduled',
      });

      expect(updateOrganizationalUnitStatus).toHaveBeenCalledWith(OU_ID, {
        suspensionPeriod: {
          start: '2026-07-01T00:00:00.000Z',
          end: '2026-08-15T00:00:00.000Z',
        },
        reason: 'INVESTIGATION',
        subreason: 'FRAUD',
        comment: 'scheduled',
      });
    });

    it('should send a null end when the end date is left empty', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit();
      mockUiEventNext.mockClear();

      wrapper.vm.openScheduleSuspensionDialog();
      const { onSubmit } = mockUiEventNext.mock.calls[0][0].data;
      await onSubmit({
        start: '2026/07/01',
        end: '',
        reason: 'INVESTIGATION',
        subreason: 'FRAUD',
        comment: null,
      });

      expect(updateOrganizationalUnitStatus).toHaveBeenCalledWith(OU_ID, {
        suspensionPeriod: {
          start: '2026-07-01T00:00:00.000Z',
          end: null,
        },
        reason: 'INVESTIGATION',
        subreason: 'FRAUD',
        comment: null,
      });
    });

    it('should pre-fill the form with the already planned suspension', async () => {
      mockedGetById.mockReset();
      mockedGetById.mockResolvedValue(
        buildOuDto({
          suspensionPeriod: {
            start: '2099-01-01T00:00:00Z',
            end: '2099-12-31T00:00:00Z',
          },
          statusReason: 'Suspension Reason A',
          statusSubreason: 'Suspension Sub-reason A.1',
          statusComment: 'planned',
        })
      );
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit();
      mockUiEventNext.mockClear();

      wrapper.vm.openScheduleSuspensionDialog();

      const { initialFormData } = mockUiEventNext.mock.calls[0][0].data;
      expect(initialFormData).toEqual({
        start: '2099/01/01',
        end: '2099/12/31',
        reason: 'Suspension Reason A',
        subreason: 'Suspension Sub-reason A.1',
        comment: 'planned',
      });
    });
  });
});
