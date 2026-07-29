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
import {
  reactivateOrganizationalUnit,
  suspendOrganizationalUnit,
} from 'src/services/OrganizationalUnitService';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OrganizationalUnitLifecyclePanel from '../../../../src/components/details/OrganizationalUnitLifecyclePanel.vue';

const mockedSuspend = vi.mocked(suspendOrganizationalUnit);
const mockedReactivate = vi.mocked(reactivateOrganizationalUnit);
const mockNotify = vi.fn();
const mockUiEventNext = vi.fn();

const OU_ID = 'test-ou-id';

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
    create: vi.fn(() => ({
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() },
      },
    })),
    isAxiosError: (err) => err?.isAxiosError === true,
    isCancel: (err) => err?.isCanceled === true,
  },
}));

vi.mock('src/services/OrganizationalUnitService', () => ({
  suspendOrganizationalUnit: vi.fn(),
  reactivateOrganizationalUnit: vi.fn(),
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
  const mockAppConfig = {
    immediateActionDelay: 60,
    organizationalUnitLifecycleFields: {
      'suspension.immediate': [
        { name: 'reason', type: 'String', input: 'List', required: true },
        { name: 'subreason', type: 'String', input: 'List', required: true },
        { name: 'comment', type: 'String', input: 'TextArea', required: false },
      ],
      'suspension.scheduled': [
        { name: 'start', type: 'String', input: 'Date', required: true },
        { name: 'end', type: 'String', input: 'Date', required: false },
        { name: 'reason', type: 'String', input: 'List', required: true },
        { name: 'subreason', type: 'String', input: 'List', required: true },
        { name: 'comment', type: 'String', input: 'TextArea', required: false },
      ],
      'suspension.modify-end': [
        { name: 'end', type: 'String', input: 'Date', required: false },
        { name: 'reason', type: 'String', input: 'List', required: true },
        { name: 'subreason', type: 'String', input: 'List', required: true },
        { name: 'comment', type: 'String', input: 'TextArea', required: false },
      ],
      'reactivation.immediate': [
        { name: 'comment', type: 'String', input: 'TextArea', required: true },
      ],
    },
  };
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

const mountPanel = (props = {}) =>
  shallowMount(OrganizationalUnitLifecyclePanel, {
    props: {
      entity: buildOuDto(),
      entityId: OU_ID,
      ...props,
    },
  });

describe('Test component: OrganizationalUnitLifecyclePanel', () => {
  let wrapper;

  beforeEach(() => {
    vi.clearAllMocks();
    mockedSuspend.mockResolvedValue(buildOuDto());
    mockedReactivate.mockResolvedValue(buildOuDto());
  });

  describe('Test computed: organizationalUnitStatus', () => {
    it('should be null while the entity is not loaded', () => {
      wrapper = mountPanel({ entity: {} });

      expect(wrapper.vm.organizationalUnitStatus).toBeNull();
    });

    it('should expose only the lifecycle fields of the entity', () => {
      wrapper = mountPanel();

      expect(wrapper.vm.organizationalUnitStatus).toMatchObject({
        isSuspended: false,
        suspensionPeriod: null,
      });
      expect(wrapper.vm.organizationalUnitStatus).not.toHaveProperty('name');
    });
  });

  describe('Test computed: actionDelay', () => {
    it('should default to 5 when appConfig.immediateActionDelay is 0 or less', () => {
      const mockAppConfig = global.mockAppConfig;
      const originalDelay = mockAppConfig.immediateActionDelay;
      mockAppConfig.immediateActionDelay = 0;

      wrapper = mountPanel();
      expect(wrapper.vm.actionDelay).toBe(5);

      mockAppConfig.immediateActionDelay = originalDelay;
    });

    it('should default to 5 when appConfig.immediateActionDelay is undefined', () => {
      const mockAppConfig = global.mockAppConfig;
      const originalDelay = mockAppConfig.immediateActionDelay;
      mockAppConfig.immediateActionDelay = undefined;

      wrapper = mountPanel();
      expect(wrapper.vm.actionDelay).toBe(5);

      mockAppConfig.immediateActionDelay = originalDelay;
    });

    it('should use the configured value when appConfig.immediateActionDelay is greater than 0', () => {
      wrapper = mountPanel();
      expect(wrapper.vm.actionDelay).toBe(60);
    });
  });

  describe('Test computed: lifecycleUi', () => {
    it('should be null while the OU is not loaded', () => {
      wrapper = mountPanel({ entity: {} });
      expect(wrapper.vm.lifecycleUi).toBeNull();
    });

    it('should project a not-suspended OU into the suspension dropdown UI state', () => {
      wrapper = mountPanel();

      const ui = wrapper.vm.lifecycleUi;
      expect(ui.showBadge).toBe(true);
      expect(ui.showSuspendedBanner).toBeFalsy();
      expect(ui.suspensionMenuItems).toEqual([
        { key: 'suspension.immediate', clickable: true },
        { key: 'suspension.scheduled', clickable: true },
      ]);
      expect(ui.activationMenuItems).toBeUndefined();
    });

    it('should project a suspended OU into the banner-only UI state without action dropdown', () => {
      wrapper = mountPanel({
        entity: buildOuDto({
          isSuspended: true,
          suspensionPeriod: { start: '2026-01-01T00:00:00Z', end: null },
        }),
      });

      const ui = wrapper.vm.lifecycleUi;
      expect(ui.showBadge).toBe(true);
      expect(ui.showSuspendedBanner).toBe(true);
      expect(ui.activationMenuItems).toBeUndefined();
      expect(ui.suspensionMenuItems).toBeUndefined();
    });
  });

  describe('Test computed: hasAnyLifecycleAction', () => {
    it('should be false while the OU is not loaded', () => {
      wrapper = mountPanel({ entity: {} });
      expect(wrapper.vm.hasAnyLifecycleAction).toBe(false);
    });

    it('should be true once at least one dropdown exposes menu items', () => {
      wrapper = mountPanel();
      expect(wrapper.vm.hasAnyLifecycleAction).toBe(true);
    });
  });

  describe('Test function: onLifecycleActionClick', () => {
    beforeEach(() => {
      wrapper = mountPanel();
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
      wrapper = mountPanel({
        entity: buildOuDto({
          isSuspended: true,
          suspensionPeriod: {
            start: '2026-05-23T00:00:00Z',
            end: null,
          },
        }),
      });
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
    it('should open the edit-suspension-end form dialog', () => {
      wrapper = mountPanel({
        entity: buildOuDto({
          isSuspended: true,
          suspensionPeriod: {
            start: '2026-01-01T00:00:00Z',
            end: '2026-12-31T00:00:00Z',
          },
        }),
      });
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

    it('should not open the dialog when there is no current suspension start', () => {
      wrapper = mountPanel({
        entity: buildOuDto({
          isSuspended: false,
          suspensionPeriod: { start: null, end: null },
        }),
      });
      mockUiEventNext.mockClear();

      wrapper.vm.onModifySuspensionEnd();

      expect(mockUiEventNext).not.toHaveBeenCalled();
    });

    it('should convert the localized end date to an ISO string before submitting', async () => {
      wrapper = mountPanel({
        entity: buildOuDto({
          isSuspended: true,
          suspensionPeriod: {
            start: '2026-01-01T00:00:00Z',
            end: '2026-12-31T00:00:00Z',
          },
        }),
      });
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
      wrapper = mountPanel();
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
      wrapper = mountPanel();
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

  describe('Test function: submitStatus', () => {
    beforeEach(() => {
      wrapper = mountPanel();
      mockUiEventNext.mockClear();
    });

    it('should notify and emit the reload event on success', async () => {
      const statusUpdate = vi.fn().mockResolvedValueOnce(buildOuDto());

      await wrapper.vm.submitStatus(statusUpdate, 'success.suspended');

      expect(statusUpdate).toHaveBeenCalledOnce();
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'positive',
        message: 'success.suspended',
      });
      expect(mockUiEventNext).toHaveBeenCalledWith({
        key: 'organizational-unit-status-updated',
        data: null,
      });
    });

    it('should notify with errors.validation and rethrow on a 400 axios error', async () => {
      const axiosError = {
        isAxiosError: true,
        response: { status: 400 },
      };
      const statusUpdate = vi.fn().mockRejectedValueOnce(axiosError);

      await expect(
        wrapper.vm.submitStatus(statusUpdate, 'success.suspended')
      ).rejects.toEqual(axiosError);

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.validation',
      });
      expect(mockUiEventNext).not.toHaveBeenCalled();
    });

    it('should notify with errors.generic and rethrow on any other error', async () => {
      const genericError = new Error('boom');
      const statusUpdate = vi.fn().mockRejectedValueOnce(genericError);

      await expect(
        wrapper.vm.submitStatus(statusUpdate, 'success.suspended')
      ).rejects.toEqual(genericError);

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.generic',
      });
    });
  });
});
