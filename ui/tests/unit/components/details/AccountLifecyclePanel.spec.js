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
  deactivateAccount,
  reactivateAccount,
  setAccountValidity,
  suspendAccount,
} from 'src/services/AccountService';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AccountLifecyclePanel from '../../../../src/components/details/AccountLifecyclePanel.vue';

const mockedSuspendAccount = vi.mocked(suspendAccount);
const mockedDeactivateAccount = vi.mocked(deactivateAccount);
const mockedReactivateAccount = vi.mocked(reactivateAccount);
const mockedSetAccountValidity = vi.mocked(setAccountValidity);

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
  getI18nInstance: vi.fn(() => ({ global: { t: mockGlobalT } })),
  merge: vi.fn((a, b) => ({ ...a, ...b })),
  uiEventSubject: { next: mockUiEventSubjectNext },
}));

vi.mock('axios', () => ({
  default: {
    isAxiosError: (err) => err?.isAxiosError === true,
  },
}));

vi.mock('src/services/AccountService', () => ({
  suspendAccount: vi.fn(),
  deactivateAccount: vi.fn(),
  reactivateAccount: vi.fn(),
  setAccountValidity: vi.fn(),
}));

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: vi.fn((v) => v) }),
}));

vi.mock('boot/config', () => {
  const mockAppConfig = {
    immediateActionDelay: 60,
    accountLifecycleFields: {
      'suspension.immediate': [],
      'suspension.scheduled': [],
      'suspension.modify': [],
      'deactivation.immediate': [],
      'deactivation.scheduled': [],
      'deactivation.modify': [],
      'reactivation.immediate': [],
      'revalidation.immediate': [],
      'revalidation.scheduled': [],
      'activation.scheduled': [],
    },
  };
  global.mockAppConfig = mockAppConfig;
  return { appConfig: mockAppConfig };
});

const buildEntity = (overrides = {}) => ({
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
  ...overrides,
});

const mountPanel = (props = {}) =>
  shallowMount(AccountLifecyclePanel, {
    props: {
      entity: buildEntity(),
      entityId: 'test-account-id',
      ...props,
    },
  });

describe('Test component: AccountLifecyclePanel', () => {
  let wrapper;

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Test computed: accountStatus', () => {
    it('should be null while the entity is not loaded', () => {
      wrapper = mountPanel({ entity: {} });

      expect(wrapper.vm.accountStatus).toBeNull();
    });

    it('should expose only the lifecycle fields of the entity', () => {
      wrapper = mountPanel();

      expect(wrapper.vm.accountStatus).toMatchObject({
        status: 'ACTIVE',
        validityPeriod: { start: '2026-01-01T00:00:00Z', end: null },
        suspensionPeriod: { start: null, end: null },
        activationAt: '2026-01-01T00:00:00Z',
      });
      expect(wrapper.vm.accountStatus).not.toHaveProperty('firstname');
    });
  });

  describe('Test computed: lifecycleUi', () => {
    it('should be null while the entity is not loaded', () => {
      wrapper = mountPanel({ entity: {} });

      expect(wrapper.vm.lifecycleUi).toBeNull();
    });

    it('should expose the projected lifecycle UI once the entity is loaded', () => {
      wrapper = mountPanel();

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
    it('should be false while the entity is not loaded', () => {
      wrapper = mountPanel({ entity: {} });

      expect(wrapper.vm.hasAnyLifecycleAction).toBe(false);
    });

    it('should be true when at least one family exposes menu items', () => {
      wrapper = mountPanel();

      expect(wrapper.vm.hasAnyLifecycleAction).toBe(true);
    });
  });

  describe('Test function: onLifecycleAction', () => {
    const actionCases = [
      ['activation.immediate', 'confirmation'],
      ['suspension.immediate', 'form'],
      ['deactivation.immediate', 'form'],
      ['reactivation.immediate', 'form'],
      ['activation.scheduled', 'form'],
      ['deactivation.scheduled', 'form'],
      ['deactivation.modify', 'form'],
      ['suspension.scheduled', 'form'],
      ['suspension.modify', 'form'],
    ];

    it.each(actionCases)(
      'should emit a "%s" uiEvent with key "%s" when action is dispatched as string',
      (actionKey, expectedEventKey) => {
        wrapper = mountPanel();
        mockUiEventSubjectNext.mockClear();

        wrapper.vm.onLifecycleAction(actionKey);

        expect(mockUiEventSubjectNext).toHaveBeenCalledOnce();
        expect(mockUiEventSubjectNext.mock.calls[0][0].key).toBe(
          expectedEventKey
        );
      }
    );

    it('should extract the key from a DropdownClickPayload object', () => {
      wrapper = mountPanel();
      mockUiEventSubjectNext.mockClear();

      wrapper.vm.onLifecycleAction({ key: 'activation.immediate' });

      expect(mockUiEventSubjectNext).toHaveBeenCalledOnce();
    });

    it('should notify with errors.status for an unknown action key', () => {
      wrapper = mountPanel();

      wrapper.vm.onLifecycleAction('unknown.action');

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.status',
      });
      expect(mockUiEventSubjectNext).not.toHaveBeenCalled();
    });

    it.each([
      ['suspension.immediate', 'AccountSuspensionActions.FormDialog.immediate'],
      [
        'deactivation.immediate',
        'AccountDeactivationActions.FormDialog.immediate',
      ],
      [
        'reactivation.immediate',
        'AccountReactivationActions.FormDialog.immediate',
      ],
      ['activation.scheduled', 'AccountActivationActions.FormDialog.scheduled'],
      [
        'deactivation.scheduled',
        'AccountDeactivationActions.FormDialog.scheduled',
      ],
      ['deactivation.modify', 'AccountDeactivationActions.FormDialog.modify'],
      ['suspension.scheduled', 'AccountSuspensionActions.FormDialog.scheduled'],
      ['suspension.modify', 'AccountSuspensionActions.FormDialog.modify'],
    ])(
      'should pass the correct i18nScope for action "%s"',
      (actionKey, expectedI18nScope) => {
        wrapper = mountPanel();
        mockUiEventSubjectNext.mockClear();

        wrapper.vm.onLifecycleAction(actionKey);

        expect(mockUiEventSubjectNext.mock.calls[0][0].data.i18nScope).toBe(
          expectedI18nScope
        );
      }
    );
  });

  describe('Test function: updateAccountStatus', () => {
    beforeEach(() => {
      wrapper = mountPanel();
    });

    it('should notify and emit the reload event on success', async () => {
      const statusUpdate = vi.fn().mockResolvedValueOnce(buildEntity());

      await wrapper.vm.updateAccountStatus(statusUpdate);

      expect(statusUpdate).toHaveBeenCalledOnce();
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'positive',
        message: 'updateStatusSuccess',
      });
      expect(mockUiEventSubjectNext).toHaveBeenCalledWith({
        key: 'account-status-updated',
        data: null,
      });
    });

    it('should notify with the API error message and rethrow on axios error', async () => {
      const axiosError = {
        isAxiosError: true,
        response: { data: { error: 'Conflict detected' } },
      };
      const statusUpdate = vi.fn().mockRejectedValueOnce(axiosError);

      await expect(
        wrapper.vm.updateAccountStatus(statusUpdate)
      ).rejects.toEqual(axiosError);

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'Conflict detected',
      });
      expect(mockUiEventSubjectNext).not.toHaveBeenCalled();
    });

    it('should fall back to errors.status and rethrow when axios error has no backend message', async () => {
      const axiosError = {
        isAxiosError: true,
        response: { data: {} },
      };
      const statusUpdate = vi.fn().mockRejectedValueOnce(axiosError);

      await expect(
        wrapper.vm.updateAccountStatus(statusUpdate)
      ).rejects.toEqual(axiosError);

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.status',
      });
    });

    it('should notify with errors.status and rethrow on non-axios error', async () => {
      const genericError = new Error('network failure');
      const statusUpdate = vi.fn().mockRejectedValueOnce(genericError);

      await expect(
        wrapper.vm.updateAccountStatus(statusUpdate)
      ).rejects.toEqual(genericError);

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.status',
      });
    });
  });

  describe('Test functions: lifecycle action dialogs', () => {
    const FIXED_NOW = new Date('2026-05-28T10:00:00.000Z');
    const FIXED_NOW_PLUS_1H = '2026-05-28T11:00:00.000Z';

    beforeEach(() => {
      vi.useFakeTimers();
      vi.setSystemTime(FIXED_NOW);
      mockedSuspendAccount.mockResolvedValue(buildEntity());
      mockedDeactivateAccount.mockResolvedValue(buildEntity());
      mockedReactivateAccount.mockResolvedValue(buildEntity());
      mockedSetAccountValidity.mockResolvedValue(buildEntity());
      wrapper = mountPanel();
      mockUiEventSubjectNext.mockClear();
      mockNotify.mockClear();
      mockScopedT.mockClear();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    describe('Test computed: actionDelay', () => {
      it('should default to 5 minutes when appConfig.immediateActionDelay is 0 or less', () => {
        const mockAppConfig = global.mockAppConfig;
        const originalDelay = mockAppConfig.immediateActionDelay;
        mockAppConfig.immediateActionDelay = 0;

        wrapper = mountPanel();
        expect(wrapper.vm.actionDelay).toBe(5);

        mockAppConfig.immediateActionDelay = originalDelay;
      });

      it('should default to 5 minutes when appConfig.immediateActionDelay is undefined', () => {
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

    describe('Test function: immediateActivation', () => {
      it('should open a confirmation dialog with correct title and content', () => {
        wrapper.vm.onLifecycleAction('activation.immediate');

        const { data } = mockUiEventSubjectNext.mock.calls[0][0];

        expect(data.title).toBe(
          'AccountActivationActions.ConfirmationDialog.immediate.title'
        );
        expect(data.content).toBe(
          'AccountActivationActions.ConfirmationDialog.immediate.content'
        );
      });

      it('should call updateAccountStatus with future validityPeriodStart on confirm', async () => {
        wrapper.vm.onLifecycleAction('activation.immediate');
        const { onConfirm } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onConfirm();

        expect(setAccountValidity).toHaveBeenCalledWith('test-account-id', {
          validityStart: FIXED_NOW_PLUS_1H,
        });
        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'immediateActivationSuccess',
        });
      });
    });

    describe('Test function: immediateSuspension', () => {
      it('should open a form dialog with correct title and content', () => {
        wrapper.vm.onLifecycleAction('suspension.immediate');

        const { data } = mockUiEventSubjectNext.mock.calls[0][0];

        expect(data.title).toBe(
          'AccountSuspensionActions.FormDialog.immediate.title'
        );
        expect(data.content).toBe(
          'AccountSuspensionActions.FormDialog.immediate.content'
        );
      });

      it('should call updateAccountStatus with future suspensionPeriodStart on submit', async () => {
        wrapper.vm.onLifecycleAction('suspension.immediate');
        const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onSubmit({ statusReason: 'INVESTIGATION' });

        expect(suspendAccount).toHaveBeenCalledWith(
          'test-account-id',
          expect.objectContaining({
            suspensionPeriod: expect.objectContaining({
              start: FIXED_NOW_PLUS_1H,
            }),
            reason: 'INVESTIGATION',
          })
        );
        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'immediateSuspensionSuccess',
        });
      });
    });

    describe('Test function: immediateDeactivation', () => {
      it('should open a form dialog with correct title and content', () => {
        wrapper.vm.onLifecycleAction('deactivation.immediate');

        const { data } = mockUiEventSubjectNext.mock.calls[0][0];

        expect(data.title).toBe(
          'AccountDeactivationActions.FormDialog.immediate.title'
        );
        expect(data.content).toBe(
          'AccountDeactivationActions.FormDialog.immediate.content'
        );
      });

      it('should call updateAccountStatus with future validityPeriodEnd on submit', async () => {
        wrapper.vm.onLifecycleAction('deactivation.immediate');
        const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onSubmit({ statusReason: 'INVESTIGATION' });

        expect(deactivateAccount).toHaveBeenCalledWith(
          'test-account-id',
          expect.objectContaining({
            deactivationAt: FIXED_NOW_PLUS_1H,
            reason: 'INVESTIGATION',
          })
        );
        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'immediateDeactivationSuccess',
        });
      });
    });

    describe('Test function: immediateReactivation', () => {
      it('should open a form dialog with correct title and content', () => {
        wrapper.vm.onLifecycleAction('reactivation.immediate');

        const { data } = mockUiEventSubjectNext.mock.calls[0][0];

        expect(data.title).toBe(
          'AccountReactivationActions.FormDialog.immediate.title'
        );
        expect(data.content).toBe(
          'AccountReactivationActions.FormDialog.immediate.content'
        );
      });

      it('should call reactivateAccount with the submitted comment on submit', async () => {
        wrapper.vm.onLifecycleAction('reactivation.immediate');
        const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onSubmit({ statusComment: 'Investigation closed' });

        expect(reactivateAccount).toHaveBeenCalledWith('test-account-id', {
          comment: 'Investigation closed',
        });
        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'immediateReactivationSuccess',
        });
      });
    });

    describe('Test function: scheduledActivation', () => {
      it('should open a form dialog with correct title and content', () => {
        wrapper.vm.onLifecycleAction('activation.scheduled');

        const { data } = mockUiEventSubjectNext.mock.calls[0][0];

        expect(data.title).toBe(
          'AccountActivationActions.FormDialog.scheduled.title'
        );
        expect(data.content).toBe(
          'AccountActivationActions.FormDialog.scheduled.content'
        );
      });

      it('should call updateAccountStatus with form validityPeriodStart as success date on submit', async () => {
        wrapper.vm.onLifecycleAction('activation.scheduled');
        const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onSubmit({ validityPeriodStart: '2026-07-01T00:00:00.000Z' });

        expect(setAccountValidity).toHaveBeenCalledWith('test-account-id', {
          validityStart: '2026-07-01T00:00:00.000Z',
        });
        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'scheduledActivationSuccess',
        });
      });

      it('should not interpolate a date in the success message when validityPeriodStart is null', async () => {
        wrapper.vm.onLifecycleAction('activation.scheduled');
        const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onSubmit({ validityPeriodStart: null });

        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'scheduledActivationSuccess',
        });
        expect(mockScopedT).toHaveBeenLastCalledWith(
          'scheduledActivationSuccess',
          { count: 60 }
        );
      });
    });

    describe('Test function: scheduledDeactivation', () => {
      it('should open a form dialog with correct title and content', () => {
        wrapper.vm.onLifecycleAction('deactivation.scheduled');

        const { data } = mockUiEventSubjectNext.mock.calls[0][0];

        expect(data.title).toBe(
          'AccountDeactivationActions.FormDialog.scheduled.title'
        );
        expect(data.content).toBe(
          'AccountDeactivationActions.FormDialog.scheduled.content'
        );
      });

      it('should call updateAccountStatus with form validityPeriodEnd as success date on submit', async () => {
        wrapper.vm.onLifecycleAction('deactivation.scheduled');
        const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onSubmit({ validityPeriodEnd: '2026-08-01T00:00:00.000Z' });

        expect(deactivateAccount).toHaveBeenCalledWith(
          'test-account-id',
          expect.objectContaining({
            deactivationAt: '2026-08-01T00:00:00.000Z',
          })
        );
        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'scheduledDeactivationSuccess',
        });
      });

      it('should not interpolate a date in the success message when validityPeriodEnd is null', async () => {
        wrapper.vm.onLifecycleAction('deactivation.scheduled');
        const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onSubmit({ validityPeriodEnd: null });

        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'scheduledDeactivationSuccess',
        });
        expect(mockScopedT).toHaveBeenLastCalledWith(
          'scheduledDeactivationSuccess',
          { count: 60 }
        );
      });
    });

    describe('Test function: modifyDeactivation', () => {
      it('should open a form dialog with correct title and content', () => {
        wrapper.vm.onLifecycleAction('deactivation.modify');

        const { data } = mockUiEventSubjectNext.mock.calls[0][0];

        expect(data.title).toBe(
          'AccountDeactivationActions.FormDialog.modify.title'
        );
        expect(data.content).toBe(
          'AccountDeactivationActions.FormDialog.modify.content'
        );
      });

      it('should call updateAccountStatus with form validityPeriodEnd as success date on submit', async () => {
        wrapper.vm.onLifecycleAction('deactivation.modify');
        const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onSubmit({ validityPeriodEnd: '2026-08-01T00:00:00.000Z' });

        expect(deactivateAccount).toHaveBeenCalledWith(
          'test-account-id',
          expect.objectContaining({
            deactivationAt: '2026-08-01T00:00:00.000Z',
          })
        );
        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'modifyDeactivationSuccess',
        });
      });

      it('should not interpolate a date in the success message when validityPeriodEnd is null', async () => {
        wrapper.vm.onLifecycleAction('deactivation.modify');
        const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onSubmit({ validityPeriodEnd: null });

        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'modifyDeactivationSuccess',
        });
        expect(mockScopedT).toHaveBeenLastCalledWith(
          'modifyDeactivationSuccess',
          { count: 60 }
        );
      });
    });

    describe('Test function: scheduledSuspension', () => {
      it('should open a form dialog with correct title and content', () => {
        wrapper.vm.onLifecycleAction('suspension.scheduled');

        const { data } = mockUiEventSubjectNext.mock.calls[0][0];

        expect(data.title).toBe(
          'AccountSuspensionActions.FormDialog.scheduled.title'
        );
        expect(data.content).toBe(
          'AccountSuspensionActions.FormDialog.scheduled.content'
        );
      });

      it('should call updateAccountStatus with form suspensionPeriodStart as success date on submit', async () => {
        wrapper.vm.onLifecycleAction('suspension.scheduled');
        const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onSubmit({ suspensionPeriodStart: '2026-07-15T00:00:00.000Z' });

        expect(suspendAccount).toHaveBeenCalledWith(
          'test-account-id',
          expect.objectContaining({
            suspensionPeriod: expect.objectContaining({
              start: '2026-07-15T00:00:00.000Z',
            }),
          })
        );
        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'scheduledSuspensionSuccess',
        });
      });

      it('should not interpolate a date in the success message when suspensionPeriodStart is null', async () => {
        wrapper.vm.onLifecycleAction('suspension.scheduled');
        const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onSubmit({ suspensionPeriodStart: null });

        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'scheduledSuspensionSuccess',
        });
        expect(mockScopedT).toHaveBeenLastCalledWith(
          'scheduledSuspensionSuccess',
          { count: 60 }
        );
      });
    });

    describe('Test function: modifySuspension', () => {
      it('should open a form dialog with correct title and content', () => {
        wrapper.vm.onLifecycleAction('suspension.modify');

        const { data } = mockUiEventSubjectNext.mock.calls[0][0];

        expect(data.title).toBe(
          'AccountSuspensionActions.FormDialog.modify.title'
        );
        expect(data.content).toBe(
          'AccountSuspensionActions.FormDialog.modify.content'
        );
      });

      it('should call updateAccountStatus with form suspensionPeriodStart as success date on submit', async () => {
        wrapper.vm.onLifecycleAction('suspension.modify');
        const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onSubmit({ suspensionPeriodStart: '2026-07-15T00:00:00.000Z' });

        expect(suspendAccount).toHaveBeenCalledWith(
          'test-account-id',
          expect.objectContaining({
            suspensionPeriod: expect.objectContaining({
              start: '2026-07-15T00:00:00.000Z',
            }),
          })
        );
        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'modifySuspensionSuccess',
        });
      });

      it('should not interpolate a date in the success message when suspensionPeriodStart is null', async () => {
        wrapper.vm.onLifecycleAction('suspension.modify');
        const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

        await onSubmit({ suspensionPeriodStart: null });

        expect(mockNotify).toHaveBeenCalledWith({
          type: 'positive',
          message: 'modifySuspensionSuccess',
        });
        expect(mockScopedT).toHaveBeenLastCalledWith(
          'modifySuspensionSuccess',
          { count: 60 }
        );
      });
    });
  });
});
