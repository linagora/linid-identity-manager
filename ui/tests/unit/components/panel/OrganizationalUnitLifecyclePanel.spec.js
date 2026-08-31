/*
 * Copyright (C) 2026 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
 * which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
 * of the interface window, the display of the “You are using the Open Source and free version of LinID™, powered by
 * Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!” infobox and in the e-mails
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
import OrganizationalUnitLifecyclePanel from '../../../../src/components/panel/OrganizationalUnitLifecyclePanel.vue';
import {
  mockToDate,
  mockToDateISO,
  mockToDayJs,
} from '../../helpers/mockCommonMapper.js';

const mockedSuspendOrganizationalUnit = vi.mocked(suspendOrganizationalUnit);
const mockedReactivateOrganizationalUnit = vi.mocked(
  reactivateOrganizationalUnit
);

const { mockNotify, mockUiEventSubjectNext, mockGlobalT, mockScopedT } =
  vi.hoisted(() => ({
    mockNotify: vi.fn(),
    mockUiEventSubjectNext: vi.fn(),
    mockGlobalT: vi.fn((v) => v),
    mockScopedT: vi.fn((v) => v),
  }));

vi.mock('@linagora/linid-im-front-corelib', () => ({
  loadAsyncComponent: vi.fn(() => null),
  useNotify: () => ({
    Notify: mockNotify,
  }),
  useScopedI18n: () => ({
    t: mockScopedT,
  }),
  useUiDesign: () => ({
    ui: vi.fn(() => ({})),
  }),
  getI18nInstance: vi.fn(() => ({ global: { t: mockGlobalT } })),
  merge: vi.fn((a, b) => ({ ...a, ...b })),
  uiEventSubject: { next: mockUiEventSubjectNext },
  useCommonMapper: () => ({
    toDate: mockToDate('DD/MM/YYYY'),
    toDateISO: mockToDateISO(),
    toDayJs: mockToDayJs(),
  }),
}));

vi.mock('axios', () => ({
  default: {
    isAxiosError: (err) => err?.isAxiosError === true,
  },
}));

vi.mock('src/services/OrganizationalUnitService', () => ({
  suspendOrganizationalUnit: vi.fn(),
  reactivateOrganizationalUnit: vi.fn(),
}));

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: vi.fn((v) => v) }),
}));

vi.mock('boot/config', () => {
  const mockAppConfig = {
    immediateActionDelay: 60,
    organizationalUnitLifecycleFields: {
      'suspension.immediate': [],
      'suspension.scheduled': [],
      'suspension.modify-end': [],
      'reactivation.immediate': [],
    },
  };
  return { appConfig: mockAppConfig };
});

const activeOrganizationalUnitDto = {
  id: 'test-ou-id',
  name: 'Company A',
  type: 'COMPANY',
  createdBy: 'Alice Creator',
  updatedBy: 'Bob Updater',
  insertDate: '2026-04-15T12:00:24.814930Z',
  updateDate: '2026-04-16T09:30:00.000000Z',
  suspensionPeriod: null,
  isSuspended: false,
  extraParameters: {},
};

const suspendedOrganizationalUnitDto = {
  ...activeOrganizationalUnitDto,
  suspensionPeriod: { start: '2026-05-01T00:00:00Z', end: null },
  isSuspended: true,
};

/**
 * Mounts the panel with the given entity, mimicking the zone renderer of the generic details page.
 *
 * @param entity - The raw organizational unit entity provided by the hosting page.
 * @returns The shallow-mounted wrapper.
 */
function mountPanel(entity = activeOrganizationalUnitDto) {
  return shallowMount(OrganizationalUnitLifecyclePanel, {
    props: {
      entity,
      uiNamespace: 'moduleOrganizationalUnitDetailsPage',
    },
  });
}

describe('Test component: OrganizationalUnitLifecyclePanel', () => {
  let wrapper;

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Test computed: organizationalUnitId', () => {
    it('should expose the entity id', () => {
      wrapper = mountPanel();

      expect(wrapper.vm.organizationalUnitId).toBe('test-ou-id');
    });
  });

  describe('Test computed: organizationalUnitStatus', () => {
    it('should be null while the entity is not resolved', () => {
      wrapper = mountPanel({});

      expect(wrapper.vm.organizationalUnitStatus).toBeNull();
    });

    it('should expose only the lifecycle fields of the resolved entity', () => {
      wrapper = mountPanel(suspendedOrganizationalUnitDto);

      expect(wrapper.vm.organizationalUnitStatus).toEqual({
        suspensionPeriod: { start: '2026-05-01T00:00:00Z', end: null },
        isSuspended: true,
      });
    });
  });

  describe('Test computed: lifecycleUi', () => {
    it('should be null while the entity is not resolved', () => {
      wrapper = mountPanel({});

      expect(wrapper.vm.lifecycleUi).toBeNull();
    });

    it('should expose the suspension actions for a non-suspended entity', () => {
      wrapper = mountPanel();

      const ui = wrapper.vm.lifecycleUi;
      expect(ui.showBadge).toBe(true);
      expect(ui.showSuspendedBanner).toBeUndefined();
      expect(ui.suspensionMenuItems).toEqual([
        { key: 'suspension.immediate', clickable: true },
        { key: 'suspension.scheduled', clickable: true },
      ]);
      expect(ui.activationMenuItems).toBeUndefined();
    });

    it('should expose the suspended banner without dropdown actions for a suspended entity', () => {
      wrapper = mountPanel(suspendedOrganizationalUnitDto);

      const ui = wrapper.vm.lifecycleUi;
      expect(ui.showSuspendedBanner).toBe(true);
      expect(ui.suspensionMenuItems).toBeUndefined();
      expect(ui.activationMenuItems).toBeUndefined();
    });
  });

  describe('Test computed: hasAnyLifecycleAction', () => {
    it('should be false while the entity is not resolved', () => {
      wrapper = mountPanel({});

      expect(wrapper.vm.hasAnyLifecycleAction).toBe(false);
    });

    it('should be true when at least one dropdown exposes menu items', () => {
      wrapper = mountPanel();

      expect(wrapper.vm.hasAnyLifecycleAction).toBe(true);
    });

    it('should be false for a suspended entity whose actions live in the banner', () => {
      wrapper = mountPanel(suspendedOrganizationalUnitDto);

      expect(wrapper.vm.hasAnyLifecycleAction).toBe(false);
    });
  });

  describe('Test function: onLifecycleActionClick', () => {
    const actionCases = [
      ['suspension.immediate'],
      ['suspension.scheduled'],
      ['reactivation.immediate'],
    ];

    it.each(actionCases)(
      'should open a form dialog when the "%s" action is dispatched',
      (actionKey) => {
        wrapper = mountPanel();

        wrapper.vm.onLifecycleActionClick({ key: actionKey });

        expect(mockUiEventSubjectNext).toHaveBeenCalledOnce();
        expect(mockUiEventSubjectNext.mock.calls[0][0].key).toBe('form');
      }
    );

    it('should ignore an unknown action key', () => {
      wrapper = mountPanel();

      wrapper.vm.onLifecycleActionClick({ key: 'unknown.action' });

      expect(mockUiEventSubjectNext).not.toHaveBeenCalled();
      expect(mockNotify).not.toHaveBeenCalled();
    });
  });

  describe('Test function: openImmediateSuspensionDialog', () => {
    it('should open a form dialog with correct title and content', () => {
      wrapper = mountPanel();

      wrapper.vm.onLifecycleActionClick({ key: 'suspension.immediate' });

      const { data } = mockUiEventSubjectNext.mock.calls[0][0];
      expect(data.title).toBe('OrganizationalUnitSuspendDialog.title');
      expect(data.content).toBe('OrganizationalUnitSuspendDialog.content');
    });

    it('should suspend with a near-future suspension start on submit', async () => {
      mockedSuspendOrganizationalUnit.mockResolvedValue(
        suspendedOrganizationalUnitDto
      );
      wrapper = mountPanel();
      wrapper.vm.onLifecycleActionClick({ key: 'suspension.immediate' });
      const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

      await onSubmit({ reason: 'Reason A', subreason: 'Sub-reason A.1' });

      expect(suspendOrganizationalUnit).toHaveBeenCalledWith(
        'test-ou-id',
        expect.objectContaining({
          reason: 'Reason A',
          subreason: 'Sub-reason A.1',
          suspensionPeriod: expect.objectContaining({
            start: expect.stringMatching(/^\d{4}-\d{2}-\d{2}T/),
            end: null,
          }),
        })
      );
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'positive',
        message: 'success.suspended',
      });
    });
  });

  describe('Test function: openScheduleSuspensionDialog', () => {
    it('should open a form dialog with correct title and content', () => {
      wrapper = mountPanel();

      wrapper.vm.onLifecycleActionClick({ key: 'suspension.scheduled' });

      const { data } = mockUiEventSubjectNext.mock.calls[0][0];
      expect(data.title).toBe(
        'OrganizationalUnitScheduleSuspensionDialog.title'
      );
    });

    it('should suspend with the form suspension period on submit', async () => {
      mockedSuspendOrganizationalUnit.mockResolvedValue(
        suspendedOrganizationalUnitDto
      );
      wrapper = mountPanel();
      wrapper.vm.onLifecycleActionClick({ key: 'suspension.scheduled' });
      const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

      await onSubmit({
        start: '2026-07-15T00:00:00.000Z',
        end: null,
        reason: 'Reason A',
        subreason: 'Sub-reason A.1',
      });

      expect(suspendOrganizationalUnit).toHaveBeenCalledWith(
        'test-ou-id',
        expect.objectContaining({
          suspensionPeriod: expect.objectContaining({
            start: '2026-07-15T00:00:00.000Z',
          }),
        })
      );
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'positive',
        message: 'success.scheduled',
      });
    });
  });

  describe('Test function: onClearSuspension', () => {
    it('should open a form dialog with correct title and content', () => {
      wrapper = mountPanel(suspendedOrganizationalUnitDto);

      wrapper.vm.onClearSuspension();

      const { data } = mockUiEventSubjectNext.mock.calls[0][0];
      expect(data.title).toBe('OrganizationalUnitReactivateDialog.title');
    });

    it('should reactivate with the form comment on submit', async () => {
      mockedReactivateOrganizationalUnit.mockResolvedValue(
        activeOrganizationalUnitDto
      );
      wrapper = mountPanel(suspendedOrganizationalUnitDto);
      wrapper.vm.onClearSuspension();
      const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

      await onSubmit({ comment: 'Suspension lifted' });

      expect(reactivateOrganizationalUnit).toHaveBeenCalledWith('test-ou-id', {
        comment: 'Suspension lifted',
      });
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'positive',
        message: 'success.reactivated',
      });
    });
  });

  describe('Test function: onModifySuspensionEnd', () => {
    it('should do nothing when the entity carries no suspension start', () => {
      wrapper = mountPanel();

      wrapper.vm.onModifySuspensionEnd();

      expect(mockUiEventSubjectNext).not.toHaveBeenCalled();
    });

    it('should open a form dialog pre-filled with the existing suspension period', () => {
      wrapper = mountPanel(suspendedOrganizationalUnitDto);

      wrapper.vm.onModifySuspensionEnd();

      const { data } = mockUiEventSubjectNext.mock.calls[0][0];
      expect(data.title).toBe(
        'OrganizationalUnitEditSuspensionEndDialog.title'
      );
      expect(data.initialFormData).toEqual({
        start: '01/05/2026',
        end: null,
      });
    });

    it('should suspend keeping the current suspension start on submit', async () => {
      mockedSuspendOrganizationalUnit.mockResolvedValue(
        suspendedOrganizationalUnitDto
      );
      wrapper = mountPanel(suspendedOrganizationalUnitDto);
      wrapper.vm.onModifySuspensionEnd();
      const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

      await onSubmit({ end: '2026-06-01T00:00:00.000Z' });

      expect(suspendOrganizationalUnit).toHaveBeenCalledWith(
        'test-ou-id',
        expect.objectContaining({
          suspensionPeriod: expect.objectContaining({
            start: '2026-05-01T00:00:00Z',
            end: '2026-06-01T00:00:00.000Z',
          }),
        })
      );
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'positive',
        message: 'success.endUpdated',
      });
    });
  });

  describe('Test function: submitStatus', () => {
    it('should emit the organizational-unit-status-updated uiEvent with the updated DTO on success', async () => {
      const statusUpdate = vi
        .fn()
        .mockResolvedValue(suspendedOrganizationalUnitDto);
      wrapper = mountPanel();

      await wrapper.vm.submitStatus(statusUpdate, 'success.suspended');

      expect(mockUiEventSubjectNext).toHaveBeenCalledWith({
        key: 'organizational-unit-status-updated',
        data: suspendedOrganizationalUnitDto,
      });
    });

    it('should notify with errors.validation on a 400 error and rethrow', async () => {
      const error = { isAxiosError: true, response: { status: 400 } };
      const statusUpdate = vi.fn().mockRejectedValue(error);
      wrapper = mountPanel();

      await expect(
        wrapper.vm.submitStatus(statusUpdate, 'success.suspended')
      ).rejects.toBe(error);

      expect(mockUiEventSubjectNext).not.toHaveBeenCalled();
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.validation',
      });
    });

    it('should notify with errors.status on any other error and rethrow', async () => {
      const error = new Error('boom');
      const statusUpdate = vi.fn().mockRejectedValue(error);
      wrapper = mountPanel();

      await expect(
        wrapper.vm.submitStatus(statusUpdate, 'success.suspended')
      ).rejects.toBe(error);

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.status',
      });
    });
  });
});
