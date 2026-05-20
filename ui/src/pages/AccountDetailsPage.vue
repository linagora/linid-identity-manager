<!--
  Copyright (C) 2026 Linagora

  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
  Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
  any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
  LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
  which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
  of the interface window, the display of the “You are using the Open Source and free version of LinID™, powered by
  Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!” infobox and in the e-mails
  sent with the Program, notice appended to any type of outbound messages (e.g. e-mail and meeting requests) as well
  as in the LinID Identity Manager user interface, (ii) retain all hypertext links between LinID Identity Manager
  and https://linid.org/, as well as between LINAGORA and LINAGORA.com, and (iii) refrain from infringing LINAGORA
  intellectual property rights over its trademarks and commercial brands. Other Additional Terms apply, see
  <http://www.linagora.com/licenses/> for more details.

  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
  details.

  You should have received a copy of the GNU Affero General Public License and its applicable Additional Terms for
  LinID Identity Manager along with this program. If not, see <http://www.gnu.org/licenses/> for the GNU Affero
  General Public License version 3 and <http://www.linagora.com/licenses/> for the Additional Terms applicable to the
  LinID Identity Manager software.
-->

<template>
  <!-- v8 ignore start -->
  <q-page
    class="row justify-center q-pa-md account-details-page"
    data-cy="account-details-page"
  >
    <div class="col-12 col-md-10 col-lg-10 account-details-page--content">
      <div
        class="row items-center justify-between q-mb-md account-details-page--header"
      >
        <div class="row items-center q-gutter-x-md">
          <h1
            class="q-ma-none text-h5 account-details-page--title"
            data-cy="account-details-page_title"
          >
            {{ t('title') }}
          </h1>
          <AccountStatusBadge
            v-if="lifecycleUi?.showBadge && accountStatus"
            :status="accountStatus.status"
          />
        </div>
        <div class="account-details-page--actions">
          <component
            :is="buttonsCard"
            v-if="buttonsCard"
            :ui-namespace="uiNamespace"
            :i18n-scope="i18nScope"
            :show-confirm-button="false"
            @cancel="goBack"
          />
        </div>
      </div>

      <div
        v-if="lifecycleUi && accountStatus"
        class="column q-gutter-y-sm q-mb-md account-details-page--lifecycle"
        data-cy="account-details-page_lifecycle"
      >
        <AccountSuspendedBanner
          v-if="lifecycleUi.showSuspendedBanner"
          :account-status="accountStatus"
          @clear-suspension="onLifecycleAction('reactivation.immediate')"
          @modify-suspension="onLifecycleAction('suspension.modify')"
        />

        <AccountDeactivatedWarningBanner
          v-if="lifecycleUi.showDeactivationWarningBanner"
          :account-status="accountStatus"
          @deactivate-immediate="onLifecycleAction('deactivation.immediate')"
          @modify-deactivation="onLifecycleAction('deactivation.modify')"
        />

        <AccountDeactivatedInfoText
          v-if="lifecycleUi.showWillDeactivateInfoText"
          :account-status="accountStatus"
        />

        <AccountSuspendedInfoText
          v-if="lifecycleUi.showWillSuspendInfoText"
          :account-status="accountStatus"
        />

        <AccountNotActivatedInfoText
          v-if="lifecycleUi.showNotActivatedInfoText"
        />

        <div
          v-if="hasAnyLifecycleAction"
          class="row q-gutter-x-sm account-details-page--lifecycle--actions"
          data-cy="account-lifecycle-actions"
        >
          <component
            :is="dropdownButton"
            v-if="dropdownButton && lifecycleUi.activationMenuItems?.length"
            :ui-namespace="`${uiNamespace}.activation-actions`"
            i18n-scope="AccountActivationActions"
            :items="lifecycleUi.activationMenuItems"
            data-cy="account-activation-actions"
            @item-click="onLifecycleAction"
          />
          <component
            :is="dropdownButton"
            v-if="dropdownButton && lifecycleUi.suspensionMenuItems?.length"
            :ui-namespace="`${uiNamespace}.suspension-actions`"
            i18n-scope="AccountSuspensionActions"
            :items="lifecycleUi.suspensionMenuItems"
            data-cy="account-suspension-actions"
            @item-click="onLifecycleAction"
          />
          <component
            :is="dropdownButton"
            v-if="dropdownButton && lifecycleUi.deactivationMenuItems?.length"
            :ui-namespace="`${uiNamespace}.deactivation-actions`"
            i18n-scope="AccountDeactivationActions"
            :items="lifecycleUi.deactivationMenuItems"
            data-cy="account-deactivation-actions"
            @item-click="onLifecycleAction"
          />
        </div>
      </div>

      <component
        :is="entityDetailsCard"
        v-if="entityDetailsCard"
        :entity="account ?? {}"
        :field-order="fieldsOrder"
        :is-loading="isLoading"
        :ui-namespace="uiNamespace"
        :i18n-scope="i18nScope"
        class="q-mb-md account-details-page--cards"
        data-cy="account-details-page_cards"
      />
    </div>
  </q-page>
  <!-- v8 ignore stop -->
</template>

<script setup lang="ts">
import type {
  DropdownClickPayload,
  UiEvent,
} from '@linagora/linid-im-front-corelib';
import {
  getI18nInstance,
  loadAsyncComponent,
  uiEventSubject,
  useNotify,
  useScopedI18n,
} from '@linagora/linid-im-front-corelib';
import axios from 'axios';
import { accountLifecycleUiConfiguration } from 'src/assets/accounts/accountLifecycleUiConfiguration';
import { fieldsOrder } from 'src/assets/accounts/detailsConfiguration';
import { dayjs } from 'src/boot/dayjs';
import AccountStatusBadge from 'src/components/badge/AccountStatusBadge.vue';
import AccountDeactivatedWarningBanner from 'src/components/banner/AccountDeactivatedWarningBanner.vue';
import AccountSuspendedBanner from 'src/components/banner/AccountSuspendedBanner.vue';
import AccountDeactivatedInfoText from 'src/components/text/AccountDeactivatedInfoText.vue';
import AccountNotActivatedInfoText from 'src/components/text/AccountNotActivatedInfoText.vue';
import AccountSuspendedInfoText from 'src/components/text/AccountSuspendedInfoText.vue';
import { useAccountLifecycleUi } from 'src/composables/useAccountLifecycleUi';
import { useAccountMapper } from 'src/mappers/accountMapper';
import { getAccountById, updateStatus } from 'src/services/AccountService';
import type { AccountStatusForm } from 'src/types/accounts';
import { type Account, type AccountStatus } from 'src/types/accounts';
import { computed, onMounted, ref } from 'vue';
import { type Composer } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

const pageName = 'AccountDetailsPage';
const i18nScope = pageName;
const uiNamespace = 'accounts.details-page';
const globalT = (getI18nInstance().global as Composer).t;

const route = useRoute();
const router = useRouter();
const { t } = useScopedI18n(i18nScope);
const { Notify } = useNotify();
const {
  toAccount,
  toAccountStatus,
  toAccountStatusForm,
  toAccountStatusRecord,
} = useAccountMapper();

const accountId = computed(() => route.params.id as string);

const account = ref<Account | null>(null);
const accountStatus = ref<AccountStatus | null>(null);
const isLoading = ref<boolean>(false);

const entityDetailsCard = loadAsyncComponent('catalogUI/EntityDetailsCard');
const buttonsCard = loadAsyncComponent('catalogUI/ButtonsCard');
const dropdownButton = loadAsyncComponent('catalogUI/DropdownButton');

const lifecycleUi = useAccountLifecycleUi(accountStatus);

const hasAnyLifecycleAction = computed(() =>
  Boolean(
    lifecycleUi.value?.activationMenuItems?.length ||
    lifecycleUi.value?.suspensionMenuItems?.length ||
    lifecycleUi.value?.deactivationMenuItems?.length
  )
);

/**
 * Loads the account data from the backend based on the route parameter and
 * splits the raw DTO into the page-level identity (`account`) and lifecycle
 * (`accountStatus`) projections.
 */
async function loadAccount(): Promise<void> {
  isLoading.value = true;
  try {
    const dto = await getAccountById(accountId.value);
    account.value = toAccount(dto);
    accountStatus.value = toAccountStatus(dto);
  } catch (error) {
    const errorMessageKey =
      axios.isAxiosError(error) && error.response?.status === 404
        ? 'errors.notFound'
        : 'errors.generic';
    Notify({
      type: 'negative',
      message: t(errorMessageKey),
    });
    goBack();
  } finally {
    isLoading.value = false;
  }
}

/**
 * Navigates back to the accounts list.
 */
function goBack(): void {
  router.push('/accounts');
}

onMounted(() => {
  loadAccount();
});

/**
 * Handles click events on lifecycle action menu items by opening a confirmation
 * or form dialog before executing the action.
 * @param action - The lifecycle action associated with the clicked menu item.
 */
function onLifecycleAction(action: string | DropdownClickPayload<string>) {
  const actionKey = typeof action === 'string' ? action : action.key;

  switch (actionKey) {
    case 'activation.immediate':
      immediateActivation();
      break;
    case 'suspension.immediate':
      immediateSuspension();
      break;
    case 'deactivation.immediate':
      immediateDeactivation();
      break;
    case 'reactivation.immediate':
      immediateReactivation();
      break;
    case 'activation.scheduled':
      scheduledActivation();
      break;
    case 'deactivation.scheduled':
      scheduledDeactivation();
      break;
    case 'deactivation.modify':
      modifyDeactivation();
      break;
    case 'suspension.scheduled':
      scheduledSuspension();
      break;
    case 'suspension.modify':
      modifySuspension();
      break;
    default:
      Notify({
        type: 'negative',
        message: t('errors.status'),
      });
  }
}

/**
 * Opens a confirmation dialog for the immediate activation action.
 */
function immediateActivation() {
  uiEventSubject.next({
    key: 'confirmation',
    data: {
      type: 'open',
      title: globalT(
        `AccountActivationActions.ConfirmationDialog.immediate.title`
      ),
      content: globalT(
        `AccountActivationActions.ConfirmationDialog.immediate.content`
      ),
      uiNamespace,
      i18nScope: `AccountActivationActions.ConfirmationDialog.immediate`,
      onConfirm: () =>
        updateAccountStatus(
          {
            validityPeriodStart: dayjs().add(1, 'hour').toISOString(),
          },
          'immediateActivationSuccess'
        ),
    },
  });
}

/**
 * Opens a form dialog for the immediate suspension action,
 * allowing the user to provide additional information
 * (e.g., reason for suspension) before confirming the action.
 */
function immediateSuspension() {
  const fieldKeys = accountLifecycleUiConfiguration['suspension.immediate'].map(
    (field) => field.name
  ) as (keyof AccountStatusForm)[];

  uiEventSubject.next({
    key: 'form',
    data: {
      type: 'open',
      title: globalT(`AccountSuspensionActions.FormDialog.immediate.title`),
      content: globalT(`AccountSuspensionActions.FormDialog.immediate.content`),
      uiNamespace,
      i18nScope: `AccountSuspensionActions.FormDialog.immediate`,
      formFields: accountLifecycleUiConfiguration['suspension.immediate'],
      initialFormData: accountStatus.value
        ? toAccountStatusForm(accountStatus.value, fieldKeys)
        : undefined,
      onSubmit: (formData: AccountStatusForm) =>
        updateAccountStatus(
          {
            ...formData,
            suspensionPeriodStart: dayjs().add(1, 'hour').toISOString(),
          },
          'immediateSuspensionSuccess'
        ),
    },
  } as UiEvent);
}

/**
 * Opens a form dialog for the immediate deactivation action,
 * allowing the user to provide additional information
 * (e.g., reason for deactivation) before confirming the action.
 */
function immediateDeactivation() {
  const fieldKeys = accountLifecycleUiConfiguration[
    'deactivation.immediate'
  ].map((field) => field.name) as (keyof AccountStatusForm)[];

  uiEventSubject.next({
    key: 'form',
    data: {
      type: 'open',
      title: globalT(`AccountDeactivationActions.FormDialog.immediate.title`),
      content: globalT(
        `AccountDeactivationActions.FormDialog.immediate.content`
      ),
      uiNamespace,
      i18nScope: `AccountDeactivationActions.FormDialog.immediate`,
      formFields: accountLifecycleUiConfiguration['deactivation.immediate'],
      initialFormData: accountStatus.value
        ? toAccountStatusForm(accountStatus.value, fieldKeys)
        : undefined,
      onSubmit: (formData: AccountStatusForm) =>
        updateAccountStatus(
          {
            ...formData,
            validityPeriodEnd: dayjs().add(1, 'hour').toISOString(),
          },
          'immediateDeactivationSuccess'
        ),
    },
  } as UiEvent);
}

/**
 * Opens a form dialog for the immediate reactivation action,
 * allowing the user to provide additional information
 * (e.g., reason for reactivation) before confirming the action.
 */
function immediateReactivation() {
  const fieldKeys = accountLifecycleUiConfiguration[
    'reactivation.immediate'
  ].map((field) => field.name) as (keyof AccountStatusForm)[];

  uiEventSubject.next({
    key: 'form',
    data: {
      type: 'open',
      title: globalT(`AccountReactivationActions.FormDialog.immediate.title`),
      content: globalT(
        `AccountReactivationActions.FormDialog.immediate.content`
      ),
      uiNamespace,
      i18nScope: `AccountReactivationActions.FormDialog.immediate`,
      formFields: accountLifecycleUiConfiguration['reactivation.immediate'],
      initialFormData: accountStatus.value
        ? toAccountStatusForm(accountStatus.value, fieldKeys)
        : undefined,
      onSubmit: (formData: AccountStatusForm) =>
        updateAccountStatus(
          {
            ...formData,
            suspensionPeriodEnd: dayjs().add(1, 'hour').toISOString(),
            statusReason: null,
            statusSubreason: null,
          },
          'immediateReactivationSuccess'
        ),
    },
  } as UiEvent);
}

/**
 * Opens a form dialog for the scheduled activation action,
 * allowing the user to provide additional information
 * (e.g., activation date, reason for activation) before confirming the action.
 */
function scheduledActivation() {
  const fieldKeys = accountLifecycleUiConfiguration['activation.scheduled'].map(
    (field) => field.name
  ) as (keyof AccountStatusForm)[];

  uiEventSubject.next({
    key: 'form',
    data: {
      type: 'open',
      title: globalT(`AccountActivationActions.FormDialog.scheduled.title`),
      content: globalT(`AccountActivationActions.FormDialog.scheduled.content`),
      uiNamespace,
      i18nScope: `AccountActivationActions.FormDialog.scheduled`,
      formFields: accountLifecycleUiConfiguration['activation.scheduled'],
      initialFormData: accountStatus.value
        ? toAccountStatusForm(accountStatus.value, fieldKeys)
        : undefined,
      onSubmit: (formData: AccountStatusForm) =>
        updateAccountStatus(
          formData,
          'scheduledActivationSuccess',
          formData.validityPeriodStart
        ),
    },
  } as UiEvent);
}

/**
 * Opens a form dialog for the scheduled deactivation action,
 * allowing the user to provide additional information
 * (e.g., deactivation date, reason for deactivation) before confirming the action.
 */
function scheduledDeactivation() {
  const fieldKeys = accountLifecycleUiConfiguration[
    'deactivation.scheduled'
  ].map((field) => field.name) as (keyof AccountStatusForm)[];

  uiEventSubject.next({
    key: 'form',
    data: {
      type: 'open',
      title: globalT(`AccountDeactivationActions.FormDialog.scheduled.title`),
      content: globalT(
        `AccountDeactivationActions.FormDialog.scheduled.content`
      ),
      uiNamespace,
      i18nScope: `AccountDeactivationActions.FormDialog.scheduled`,
      formFields: accountLifecycleUiConfiguration['deactivation.scheduled'],
      initialFormData: accountStatus.value
        ? toAccountStatusForm(accountStatus.value, fieldKeys)
        : undefined,
      onSubmit: (formData: AccountStatusForm) =>
        updateAccountStatus(
          formData,
          'scheduledDeactivationSuccess',
          formData.validityPeriodEnd
        ),
    },
  } as UiEvent);
}

/**
 * Opens a form dialog for the modify deactivation action,
 * allowing the user to update the scheduled deactivation date
 * and reason before confirming the action.
 */
function modifyDeactivation() {
  const fieldKeys = accountLifecycleUiConfiguration['deactivation.modify'].map(
    (field) => field.name
  ) as (keyof AccountStatusForm)[];

  uiEventSubject.next({
    key: 'form',
    data: {
      type: 'open',
      title: globalT(`AccountDeactivationActions.FormDialog.modify.title`),
      content: globalT(`AccountDeactivationActions.FormDialog.modify.content`),
      uiNamespace,
      i18nScope: `AccountDeactivationActions.FormDialog.modify`,
      formFields: accountLifecycleUiConfiguration['deactivation.modify'],
      initialFormData: accountStatus.value
        ? toAccountStatusForm(accountStatus.value, fieldKeys)
        : undefined,
      onSubmit: (formData: AccountStatusForm) =>
        updateAccountStatus(
          formData,
          'modifyDeactivationSuccess',
          formData.validityPeriodEnd
        ),
    },
  } as UiEvent);
}

/**
 * Opens a form dialog for the scheduled suspension action,
 * allowing the user to provide additional information
 * (e.g., suspension date, reason for suspension) before confirming the action.
 */
function scheduledSuspension() {
  const fieldKeys = accountLifecycleUiConfiguration['suspension.scheduled'].map(
    (field) => field.name
  ) as (keyof AccountStatusForm)[];

  uiEventSubject.next({
    key: 'form',
    data: {
      type: 'open',
      title: globalT(`AccountSuspensionActions.FormDialog.scheduled.title`),
      content: globalT(`AccountSuspensionActions.FormDialog.scheduled.content`),
      uiNamespace,
      i18nScope: `AccountSuspensionActions.FormDialog.scheduled`,
      formFields: accountLifecycleUiConfiguration['suspension.scheduled'],
      initialFormData: accountStatus.value
        ? toAccountStatusForm(accountStatus.value, fieldKeys)
        : undefined,
      onSubmit: (formData: AccountStatusForm) =>
        updateAccountStatus(
          formData,
          'scheduledSuspensionSuccess',
          formData.suspensionPeriodStart
        ),
    },
  } as UiEvent);
}

/**
 * Opens a form dialog for the modify suspension action,
 * allowing the user to update the suspension period settings
 * before confirming the action.
 */
function modifySuspension() {
  const fieldKeys = accountLifecycleUiConfiguration['suspension.modify'].map(
    (field) => field.name
  ) as (keyof AccountStatusForm)[];

  uiEventSubject.next({
    key: 'form',
    data: {
      type: 'open',
      title: globalT(`AccountSuspensionActions.FormDialog.modify.title`),
      content: globalT(`AccountSuspensionActions.FormDialog.modify.content`),
      uiNamespace,
      i18nScope: `AccountSuspensionActions.FormDialog.modify`,
      formFields: accountLifecycleUiConfiguration['suspension.modify'],
      initialFormData: accountStatus.value
        ? toAccountStatusForm(accountStatus.value, fieldKeys)
        : undefined,
      onSubmit: (formData: AccountStatusForm) =>
        updateAccountStatus(
          formData,
          'modifySuspensionSuccess',
          formData.suspensionPeriodStart
        ),
    },
  } as UiEvent);
}

/**
 * Sends a request to the backend to update the account status based on the provided
 * form data and the current account status, then updates the page state with the
 * new account information. Displays a notification in case of an error during the
 * update process.
 * @param formData - The data collected from the confirmation dialog form,
 *                   used to construct the account status update payload.
 * @param successMsgKey - Optional i18n key for the success message to display upon successful update.
 * @param dateToDisplayInSuccessMsg - Optional date to display in the success message.
 * @returns A promise that resolves once the account status has been updated
 *          and the page state has been refreshed with the new account information.
 */
async function updateAccountStatus(
  formData: AccountStatusForm,
  successMsgKey = 'updateStatusSuccess',
  dateToDisplayInSuccessMsg?: string | null
): Promise<void> {
  isLoading.value = true;

  try {
    const dto = await updateStatus(
      accountId.value,
      toAccountStatusRecord(formData)
    );
    account.value = toAccount(dto);
    accountStatus.value = toAccountStatus(dto);

    Notify({
      type: 'positive',
      message: dateToDisplayInSuccessMsg
        ? t(successMsgKey, { date: dateToDisplayInSuccessMsg })
        : t(successMsgKey),
    });
  } catch (error) {
    const errorMsg = axios.isAxiosError(error)
      ? (error.response?.data?.error ?? t('errors.status'))
      : t('errors.status');

    Notify({
      type: 'negative',
      message: errorMsg,
    });
    throw error;
  } finally {
    isLoading.value = false;
  }
}
</script>
