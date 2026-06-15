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
  reactivateOrganizationalUnit,
  suspendOrganizationalUnit,
} from 'src/services/OrganizationalUnitService';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';
import OrganizationalUnitDetailsPage from '../../../src/pages/OrganizationalUnitDetailsPage.vue';

const mockedGetById = vi.mocked(getOrganizationalUnitById);
const mockedSuspend = vi.mocked(suspendOrganizationalUnit);
const mockedReactivate = vi.mocked(reactivateOrganizationalUnit);
const mockNotify = vi.fn();
const mockUiEventNext = vi.fn();

const OU_ID = 'test-ou-id';

const mockSelectedOrganizationalUnitId = ref(OU_ID);

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
    isCancel: (err) => err?.isCanceled === true,
  },
}));

vi.mock('src/services/OrganizationalUnitService', () => ({
  getOrganizationalUnitById: vi.fn(),
  suspendOrganizationalUnit: vi.fn(),
  reactivateOrganizationalUnit: vi.fn(),
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

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: vi.fn((key) => (key === 'application.dateFormat' ? 'YYYY/MM/DD' : key)),
  }),
}));

vi.mock('boot/config', () => {
  const mockAppConfig = { immediateActionDelay: 60 };
  // Make it accessible globally for tests
  global.mockAppConfig = mockAppConfig;
  return { appConfig: mockAppConfig };
});

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
    mockSelectedOrganizationalUnitId.value = OU_ID;
    mockedGetById.mockResolvedValue(buildOuDto());
    mockedSuspend.mockResolvedValue(buildOuDto());
    mockedReactivate.mockResolvedValue(buildOuDto());
  });

  describe('Test function: loadOrganizationalUnit', () => {
    it('should retrieve OU data and split it between identity and status', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);

      await wrapper.vm.loadOrganizationalUnit(OU_ID);

      expect(getOrganizationalUnitById).toHaveBeenCalledWith(
        OU_ID,
        expect.any(AbortSignal)
      );
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

    it('should clear the panel and not call the service when no OU is selected', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await flushPromises();
      vi.clearAllMocks();

      await wrapper.vm.loadOrganizationalUnit('');

      expect(getOrganizationalUnitById).not.toHaveBeenCalled();
      expect(wrapper.vm.organizationalUnit).toBeNull();
      expect(wrapper.vm.organizationalUnitStatus).toBeNull();
    });

    it('should toggle isLoading around the API call', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await flushPromises();

      expect(wrapper.vm.isLoading).toBe(false);

      const loadPromise = wrapper.vm.loadOrganizationalUnit(OU_ID);
      expect(wrapper.vm.isLoading).toBe(true);

      await loadPromise;
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify with notFound message on 404', async () => {
      mockedGetById.mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 404 },
      });

      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit(OU_ID);

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.notFound',
      });
    });

    it('should notify generic on non-404 errors', async () => {
      mockedGetById.mockRejectedValueOnce(new Error('boom'));

      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit(OU_ID);

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.generic',
      });
    });

    it('should abort the previous request when a new load starts', async () => {
      let firstSignal;
      mockedGetById.mockImplementationOnce((id, signal) => {
        firstSignal = signal;
        return new Promise(() => {});
      });
      mockedGetById.mockResolvedValueOnce(buildOuDto({ id: 'second-ou-id' }));

      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      void wrapper.vm.loadOrganizationalUnit(OU_ID);
      await wrapper.vm.loadOrganizationalUnit('second-ou-id');

      expect(firstSignal.aborted).toBe(true);
    });

    it('should not notify when the request is canceled', async () => {
      mockedGetById.mockRejectedValueOnce({ isCanceled: true });

      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit(OU_ID);

      expect(mockNotify).not.toHaveBeenCalled();
    });
  });

  describe('Test watcher: selectedOrganizationalUnitId', () => {
    it('should reload the OU when the tree selection changes', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await flushPromises();
      vi.clearAllMocks();

      mockSelectedOrganizationalUnitId.value = 'another-ou-id';
      await flushPromises();

      expect(getOrganizationalUnitById).toHaveBeenCalledWith(
        'another-ou-id',
        expect.any(AbortSignal)
      );
    });
  });

  describe('Test hook: onMounted', () => {
    it('should load the selected OU on mount', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await flushPromises();

      expect(getOrganizationalUnitById).toHaveBeenCalledWith(
        OU_ID,
        expect.any(AbortSignal)
      );
    });
  });

  describe('Test computed: actionDelay', () => {
    it('should default to 5 when appConfig.immediateActionDelay is 0 or less', () => {
      const mockAppConfig = global.mockAppConfig;
      const originalDelay = mockAppConfig.immediateActionDelay;
      mockAppConfig.immediateActionDelay = 0;

      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      expect(wrapper.vm.actionDelay).toBe(5);

      mockAppConfig.immediateActionDelay = originalDelay;
    });

    it('should default to 5 when appConfig.immediateActionDelay is undefined', () => {
      const mockAppConfig = global.mockAppConfig;
      const originalDelay = mockAppConfig.immediateActionDelay;
      mockAppConfig.immediateActionDelay = undefined;

      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      expect(wrapper.vm.actionDelay).toBe(5);

      mockAppConfig.immediateActionDelay = originalDelay;
    });

    it('should use the configured value when appConfig.immediateActionDelay is greater than 0', () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      expect(wrapper.vm.actionDelay).toBe(60);
    });
  });

  describe('Test computed: lifecycleUi', () => {
    it('should be null while the OU is not loaded', () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      expect(wrapper.vm.lifecycleUi).toBeNull();
    });

    it('should project a not-suspended OU into the suspension dropdown UI state', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit(OU_ID);

      const ui = wrapper.vm.lifecycleUi;
      expect(ui.showBadge).toBe(true);
      expect(ui.showSuspendedBanner).toBeFalsy();
      expect(ui.suspensionMenuItems).toEqual([
        { key: 'suspension.immediate', clickable: true },
        { key: 'suspension.scheduled', clickable: true },
      ]);
      expect(ui.activationMenuItems).toBeUndefined();
    });

    it('should project a suspended OU into the banner-only UI state without action dropdown', async () => {
      mockedGetById.mockReset();
      mockedGetById.mockResolvedValue(
        buildOuDto({
          isSuspended: true,
          suspensionPeriod: { start: '2026-01-01T00:00:00Z', end: null },
        })
      );

      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit(OU_ID);

      const ui = wrapper.vm.lifecycleUi;
      expect(ui.showBadge).toBe(true);
      expect(ui.showSuspendedBanner).toBe(true);
      expect(ui.activationMenuItems).toBeUndefined();
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
      await wrapper.vm.loadOrganizationalUnit(OU_ID);
      expect(wrapper.vm.hasAnyLifecycleAction).toBe(true);
    });
  });

  describe('Test function: onLifecycleActionClick', () => {
    beforeEach(async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit(OU_ID);
      mockUiEventNext.mockClear();
    });

    it('should open the immediate suspension form dialog with reason, subreason and comment fields', () => {
      wrapper.vm.onLifecycleActionClick({ key: 'suspension.immediate' });

      const call = mockUiEventNext.mock.calls[0][0];
      expect(call.key).toBe('form');
      expect(call.data.i18nScope).toBe('OrganizationalUnitSuspendDialog');
      expect(call.data.formFields.map((f) => f.name)).toEqual([
        'reason',
        'subreason',
        'comment',
      ]);
    });

    it('should submit an immediate suspension starting in the future to avoid a past start', async () => {
      const fixedNow = new Date('2026-05-28T10:00:00.000Z');
      const fixedNowPlus1h = '2026-05-28T11:00:00.000Z';
      vi.useFakeTimers();
      vi.setSystemTime(fixedNow);

      try {
        wrapper.vm.onLifecycleActionClick({ key: 'suspension.immediate' });
        const { onSubmit } = mockUiEventNext.mock.calls[0][0].data;
        await onSubmit({
          reason: 'Suspension Reason A',
          subreason: 'Suspension Sub-reason A.1',
          comment: 'immediate',
        });

        expect(suspendOrganizationalUnit).toHaveBeenCalledWith(OU_ID, {
          suspensionPeriod: {
            start: fixedNowPlus1h,
            end: null,
          },
          reason: 'Suspension Reason A',
          subreason: 'Suspension Sub-reason A.1',
          comment: 'immediate',
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

    it('should open the reactivation form dialog with the comment field on reactivation.immediate', () => {
      wrapper.vm.onLifecycleActionClick({ key: 'reactivation.immediate' });

      const call = mockUiEventNext.mock.calls[0][0];
      expect(call.key).toBe('form');
      expect(call.data.i18nScope).toBe('OrganizationalUnitReactivateDialog');
      expect(call.data.formFields.map((f) => f.name)).toEqual(['comment']);
    });

    it('should reactivate by submitting only the comment', async () => {
      mockedGetById.mockReset();
      mockedGetById.mockResolvedValue(
        buildOuDto({
          isSuspended: true,
          suspensionPeriod: {
            start: '2026-05-23T00:00:00Z',
            end: null,
          },
        })
      );
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit(OU_ID);
      mockUiEventNext.mockClear();

      wrapper.vm.onLifecycleActionClick({ key: 'reactivation.immediate' });
      const { onSubmit } = mockUiEventNext.mock.calls[0][0].data;
      await onSubmit({ comment: 'reactivating' });

      expect(reactivateOrganizationalUnit).toHaveBeenCalledWith(OU_ID, {
        comment: 'reactivating',
      });
    });

    it('should ignore unknown action keys', () => {
      wrapper.vm.onLifecycleActionClick({ key: 'unknown.action' });

      expect(mockUiEventNext).not.toHaveBeenCalled();
    });
  });

  describe('Test function: onModifySuspensionEnd', () => {
    it('should open the edit-suspension-end form dialog', async () => {
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
      await wrapper.vm.loadOrganizationalUnit(OU_ID);
      mockUiEventNext.mockClear();

      wrapper.vm.onModifySuspensionEnd();

      const call = mockUiEventNext.mock.calls[0][0];
      expect(call.key).toBe('form');
      expect(call.data.i18nScope).toBe(
        'OrganizationalUnitEditSuspensionEndDialog'
      );
      expect(call.data.formFields).toBeDefined();
      expect(call.data.onSubmit).toBeInstanceOf(Function);
    });

    it('should not open the dialog when there is no current suspension start', async () => {
      mockedGetById.mockReset();
      mockedGetById.mockResolvedValue(
        buildOuDto({
          isSuspended: false,
          suspensionPeriod: { start: null, end: null },
        })
      );
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit(OU_ID);
      mockUiEventNext.mockClear();

      wrapper.vm.onModifySuspensionEnd();

      expect(mockUiEventNext).not.toHaveBeenCalled();
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
      await wrapper.vm.loadOrganizationalUnit(OU_ID);
      mockUiEventNext.mockClear();

      wrapper.vm.onModifySuspensionEnd();
      const { onSubmit } = mockUiEventNext.mock.calls[0][0].data;
      await onSubmit({ end: '2027/06/30' });

      expect(suspendOrganizationalUnit).toHaveBeenCalledWith(OU_ID, {
        suspensionPeriod: {
          start: '2026-01-01T00:00:00Z',
          end: '2027-06-30T00:00:00.000Z',
        },
        reason: '',
        subreason: '',
        comment: null,
      });
    });
  });

  describe('Test function: openScheduleSuspensionDialog', () => {
    it('should convert localized start and end dates to ISO strings before submitting', async () => {
      wrapper = shallowMount(OrganizationalUnitDetailsPage);
      await wrapper.vm.loadOrganizationalUnit(OU_ID);
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

      expect(suspendOrganizationalUnit).toHaveBeenCalledWith(OU_ID, {
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
      await wrapper.vm.loadOrganizationalUnit(OU_ID);
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

      expect(suspendOrganizationalUnit).toHaveBeenCalledWith(OU_ID, {
        suspensionPeriod: {
          start: '2026-07-01T00:00:00.000Z',
          end: null,
        },
        reason: 'INVESTIGATION',
        subreason: 'FRAUD',
        comment: null,
      });
    });
  });
});
