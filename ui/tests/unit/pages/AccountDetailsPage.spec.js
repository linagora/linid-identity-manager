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
  deactivateAccount,
  getAccountById,
  reactivateAccount,
  setAccountValidity,
  suspendAccount,
} from 'src/services/AccountService';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AccountDetailsPage from '../../../src/pages/AccountDetailsPage.vue';

const mockedGetAccountById = vi.mocked(getAccountById);
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
  getAccountById: vi.fn(),
  suspendAccount: vi.fn(),
  deactivateAccount: vi.fn(),
  reactivateAccount: vi.fn(),
  setAccountValidity: vi.fn(),
}));

vi.mock('src/assets/accounts/accountLifecycleUiConfiguration', () => ({
  accountLifecycleUiConfiguration: {
    'suspension.immediate': [],
    'deactivation.immediate': [],
    'reactivation.immediate': [],
    'activation.scheduled': [],
    'deactivation.scheduled': [],
    'deactivation.modify': [],
    'suspension.scheduled': [],
    'suspension.modify': [],
  },
}));

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter,
}));

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: vi.fn((v) => v) }),
}));

vi.mock('boot/config', () => {
  const mockAppConfig = { immediateActionDelay: 60 };
  // Make it accessible globally for tests
  global.mockAppConfig = mockAppConfig;
  return { appConfig: mockAppConfig };
});

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
        wrapper = shallowMount(AccountDetailsPage);
        mockUiEventSubjectNext.mockClear();

        wrapper.vm.onLifecycleAction(actionKey);

        expect(mockUiEventSubjectNext).toHaveBeenCalledOnce();
        expect(mockUiEventSubjectNext.mock.calls[0][0].key).toBe(
          expectedEventKey
        );
      }
    );

    it('should extract the key from a DropdownClickPayload object', () => {
      wrapper = shallowMount(AccountDetailsPage);
      mockUiEventSubjectNext.mockClear();

      wrapper.vm.onLifecycleAction({ key: 'activation.immediate' });

      expect(mockUiEventSubjectNext).toHaveBeenCalledOnce();
    });

    it('should notify with errors.status for an unknown action key', () => {
      wrapper = shallowMount(AccountDetailsPage);

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
        wrapper = shallowMount(AccountDetailsPage);
        mockUiEventSubjectNext.mockClear();

        wrapper.vm.onLifecycleAction(actionKey);

        expect(mockUiEventSubjectNext.mock.calls[0][0].data.i18nScope).toBe(
          expectedI18nScope
        );
      }
    );
  });

  describe('Test function: updateAccountStatus', () => {
    beforeEach(async () => {
      wrapper = shallowMount(AccountDetailsPage);
      await wrapper.vm.loadAccount();
    });

    it('should update account and accountStatus on success', async () => {
      const updatedDto = {
        id: 'test-account-id',
        firstname: 'Jane',
        lastname: 'Doe',
        email: 'jane.doe@example.com',
        createdBy: 'Alice Creator',
        updatedBy: 'Bob Updater',
        insertDate: '2026-04-15T12:00:24.814930Z',
        updateDate: '2026-05-01T00:00:00Z',
        status: 'SUSPENDED',
        validityPeriod: { start: '2026-01-01T00:00:00Z', end: null },
        suspensionPeriod: {
          start: '2026-05-01T00:00:00Z',
          end: null,
        },
        activationAt: '2026-01-01T00:00:00Z',
        statusReason: 'INVESTIGATION',
        statusSubreason: null,
        statusComment: null,
        daysBeforeDeactivation: null,
      };
      const statusUpdate = vi.fn().mockResolvedValueOnce(updatedDto);

      await wrapper.vm.updateAccountStatus(statusUpdate);

      expect(statusUpdate).toHaveBeenCalledOnce();
      expect(wrapper.vm.account).toMatchObject({ firstname: 'Jane' });
      expect(wrapper.vm.accountStatus).toMatchObject({ status: 'SUSPENDED' });
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'positive',
        message: 'updateStatusSuccess',
      });
    });

    it('should set isLoading to true during the update and false after', async () => {
      let resolveUpdate;
      const statusUpdate = vi.fn(
        () =>
          new Promise((resolve) => {
            resolveUpdate = resolve;
          })
      );

      const updatePromise = wrapper.vm.updateAccountStatus(statusUpdate);
      expect(wrapper.vm.isLoading).toBe(true);

      resolveUpdate({
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
      await updatePromise;

      expect(wrapper.vm.isLoading).toBe(false);
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

    const resolvedDto = {
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
    };

    beforeEach(async () => {
      vi.useFakeTimers();
      vi.setSystemTime(FIXED_NOW);
      mockedSuspendAccount.mockResolvedValue(resolvedDto);
      mockedDeactivateAccount.mockResolvedValue(resolvedDto);
      mockedReactivateAccount.mockResolvedValue(resolvedDto);
      mockedSetAccountValidity.mockResolvedValue(resolvedDto);
      wrapper = shallowMount(AccountDetailsPage);
      await wrapper.vm.loadAccount();
      mockUiEventSubjectNext.mockClear();
      mockNotify.mockClear();
      mockScopedT.mockClear();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    describe('Test computed: actionDelay', () => {
      it('should default to 5 minutes when appConfig.immediateActionDelay is 0 or less', () => {
        // Temporarily set appConfig to have immediateActionDelay = 0
        const mockAppConfig = global.mockAppConfig;
        const originalDelay = mockAppConfig.immediateActionDelay;
        mockAppConfig.immediateActionDelay = 0;

        wrapper = shallowMount(AccountDetailsPage);
        expect(wrapper.vm.actionDelay).toBe(5);

        // Restore original value
        mockAppConfig.immediateActionDelay = originalDelay;
      });

      it('should default to 5 minutes when appConfig.immediateActionDelay is undefined', () => {
        // Temporarily set appConfig to have immediateActionDelay = undefined
        const mockAppConfig = global.mockAppConfig;
        const originalDelay = mockAppConfig.immediateActionDelay;
        mockAppConfig.immediateActionDelay = undefined;

        wrapper = shallowMount(AccountDetailsPage);
        expect(wrapper.vm.actionDelay).toBe(5);

        // Restore original value
        mockAppConfig.immediateActionDelay = originalDelay;
      });

      it('should use the configured value when appConfig.immediateActionDelay is greater than 0', async () => {
        wrapper = shallowMount(AccountDetailsPage);
        await wrapper.vm.loadAccount();

        // The mock is set to immediateActionDelay of 60
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
          'scheduledActivationSuccess', { count: 60 }
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
        expect(mockScopedT).toHaveBeenLastCalledWith('modifySuspensionSuccess', { count: 60 });
      });
    });
  });
});
