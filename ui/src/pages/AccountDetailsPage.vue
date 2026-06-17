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
          <StatusBadge
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

        <AccountDeactivatedBanner
          v-if="lifecycleUi.showDeactivatedBanner"
          :account-status="accountStatus"
          @reactivate-immediate="onLifecycleAction('revalidation.immediate')"
          @reactivate-scheduled="onLifecycleAction('revalidation.scheduled')"
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
import type { DropdownClickPayload } from '@linagora/linid-im-front-corelib';
import {
  loadAsyncComponent,
  useNotify,
  useScopedI18n,
} from '@linagora/linid-im-front-corelib';
import axios from 'axios';
import { appConfig } from 'boot/config';
import { dayjs } from 'src/boot/dayjs';
import StatusBadge from 'src/components/badge/StatusBadge.vue';
import AccountDeactivatedBanner from 'src/components/banner/AccountDeactivatedBanner.vue';
import AccountDeactivatedWarningBanner from 'src/components/banner/AccountDeactivatedWarningBanner.vue';
import AccountSuspendedBanner from 'src/components/banner/AccountSuspendedBanner.vue';
import AccountDeactivatedInfoText from 'src/components/text/AccountDeactivatedInfoText.vue';
import AccountNotActivatedInfoText from 'src/components/text/AccountNotActivatedInfoText.vue';
import AccountSuspendedInfoText from 'src/components/text/AccountSuspendedInfoText.vue';
import { useAccountLifecycleUi } from 'src/composables/useAccountLifecycleUi';
import { useAccountMapper } from 'src/composables/useAccountMapper';
import { useLifecycleDialogs } from 'src/composables/useLifecycleDialogs';
import {
  deactivateAccount,
  getAccountById,
  reactivateAccount,
  setAccountValidity,
  suspendAccount,
} from 'src/services/AccountService';
import type { AccountDTO, AccountStatusForm } from 'src/types/accounts';
import { type Account, type AccountStatus } from 'src/types/accounts';
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const pageName = 'AccountDetailsPage';
const i18nScope = pageName;
const uiNamespace = 'accounts.details-page';
const accountLifecycleUiConfiguration = appConfig.accountLifecycleFields;
const fieldsOrder = appConfig.accountDetailsFieldsOrder;

const route = useRoute();
const router = useRouter();
const { t } = useScopedI18n(i18nScope);
const { Notify } = useNotify();
const {
  toAccount,
  toAccountStatus,
  toAccountStatusForm,
  toAccountSuspensionRecord,
  toAccountDeactivationRecord,
  toAccountReactivationRecord,
  toAccountValidityRecord,
} = useAccountMapper();

const accountId = computed(() => route.params.id as string);

const account = ref<Account | null>(null);
const accountStatus = ref<AccountStatus | null>(null);
const isLoading = ref<boolean>(false);

const entityDetailsCard = loadAsyncComponent('catalogUI/EntityDetailsCard');
const buttonsCard = loadAsyncComponent('catalogUI/ButtonsCard');
const dropdownButton = loadAsyncComponent('catalogUI/DropdownButton');

const lifecycleUi = useAccountLifecycleUi(accountStatus);

const { openFormDialog, openConfirmationDialog } =
  useLifecycleDialogs(uiNamespace);

const hasAnyLifecycleAction = computed(() =>
  Boolean(
    lifecycleUi.value?.activationMenuItems?.length ||
    lifecycleUi.value?.suspensionMenuItems?.length ||
    lifecycleUi.value?.deactivationMenuItems?.length
  )
);

const actionDelay: number =
  appConfig?.immediateActionDelay > 0 ? appConfig.immediateActionDelay : 5;

/**
 * Loads the account data from the backend based on the route parameter and splits the raw DTO into the page-level
 * identity (`account`) and lifecycle (`accountStatus`) projections.
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

/** Navigates back to the accounts list. */
function goBack(): void {
  router.push('/accounts');
}

onMounted(() => {
  loadAccount();
});

/**
 * Handles click events on lifecycle action menu items by opening a confirmation or form dialog before executing the
 * action.
 *
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
    case 'revalidation.immediate':
      immediateRevalidation();
      break;
    case 'revalidation.scheduled':
      scheduledRevalidation();
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

/** Opens a confirmation dialog for the immediate activation action. */
function immediateActivation() {
  openConfirmationDialog({
    i18nScope: 'AccountActivationActions.ConfirmationDialog.immediate',
    onConfirm: () =>
      updateAccountStatus(
        () =>
          setAccountValidity(
            accountId.value,
            toAccountValidityRecord({
              validityPeriodStart: dayjs()
                .add(actionDelay, 'minute')
                .toISOString(),
            })
          ),
        'immediateActivationSuccess'
      ),
  });
}

/** Opens a form dialog for the immediate suspension action. */
function immediateSuspension() {
  openFormDialog({
    i18nScope: 'AccountSuspensionActions.FormDialog.immediate',
    formFields: accountLifecycleUiConfiguration['suspension.immediate'],
    onSubmit: (formData: AccountStatusForm) =>
      updateAccountStatus(
        () =>
          suspendAccount(
            accountId.value,
            toAccountSuspensionRecord({
              ...formData,
              suspensionPeriodStart: dayjs()
                .add(actionDelay, 'minute')
                .toISOString(),
            })
          ),
        'immediateSuspensionSuccess'
      ),
  });
}

/** Opens a form dialog for the immediate deactivation action. */
function immediateDeactivation() {
  openFormDialog({
    i18nScope: 'AccountDeactivationActions.FormDialog.immediate',
    formFields: accountLifecycleUiConfiguration['deactivation.immediate'],
    onSubmit: (formData: AccountStatusForm) =>
      updateAccountStatus(
        () =>
          deactivateAccount(
            accountId.value,
            toAccountDeactivationRecord({
              ...formData,
              validityPeriodEnd: dayjs()
                .add(actionDelay, 'minute')
                .toISOString(),
            })
          ),
        'immediateDeactivationSuccess'
      ),
  });
}

/** Opens a form dialog for the immediate reactivation action. */
function immediateReactivation() {
  openFormDialog({
    i18nScope: 'AccountReactivationActions.FormDialog.immediate',
    formFields: accountLifecycleUiConfiguration['reactivation.immediate'],
    onSubmit: (formData: AccountStatusForm) =>
      updateAccountStatus(
        () =>
          reactivateAccount(
            accountId.value,
            toAccountReactivationRecord(formData)
          ),
        'immediateReactivationSuccess'
      ),
  });
}

/**
 * Opens a form dialog for the immediate revalidation action of a deactivated account. The user provides a mandatory
 * justification comment, and the validity period end is pushed one hour into the future so the account becomes active
 * again shortly.
 */
function immediateRevalidation() {
  openFormDialog({
    i18nScope: 'AccountRevalidationActions.FormDialog.immediate',
    formFields: accountLifecycleUiConfiguration['revalidation.immediate'],
    onSubmit: (formData: AccountStatusForm) =>
      updateAccountStatus(
        () =>
          reactivateAccount(
            accountId.value,
            toAccountReactivationRecord({
              ...formData,
              validityPeriodEnd: dayjs()
                .add(actionDelay, 'minute')
                .toISOString(),
            })
          ),
        'immediateRevalidationSuccess'
      ),
  });
}

/**
 * Opens a form dialog for the scheduled revalidation action of a deactivated account, letting the user pick a new
 * validity period end in the future.
 */
function scheduledRevalidation() {
  openFormDialog({
    i18nScope: 'AccountRevalidationActions.FormDialog.scheduled',
    formFields: accountLifecycleUiConfiguration['revalidation.scheduled'],
    onSubmit: (formData: AccountStatusForm) =>
      updateAccountStatus(
        () =>
          reactivateAccount(
            accountId.value,
            toAccountReactivationRecord(formData)
          ),
        'scheduledRevalidationSuccess',
        formData.validityPeriodEnd
      ),
  });
}

/** Opens a form dialog for the scheduled activation action. */
function scheduledActivation() {
  openFormDialog({
    i18nScope: 'AccountActivationActions.FormDialog.scheduled',
    formFields: accountLifecycleUiConfiguration['activation.scheduled'],
    onSubmit: (formData: AccountStatusForm) =>
      updateAccountStatus(
        () =>
          setAccountValidity(
            accountId.value,
            toAccountValidityRecord(formData)
          ),
        'scheduledActivationSuccess',
        formData.validityPeriodStart
      ),
  });
}

/** Opens a form dialog for the scheduled deactivation action. */
function scheduledDeactivation() {
  openFormDialog({
    i18nScope: 'AccountDeactivationActions.FormDialog.scheduled',
    formFields: accountLifecycleUiConfiguration['deactivation.scheduled'],
    onSubmit: (formData: AccountStatusForm) =>
      updateAccountStatus(
        () =>
          deactivateAccount(
            accountId.value,
            toAccountDeactivationRecord(formData)
          ),
        'scheduledDeactivationSuccess',
        formData.validityPeriodEnd
      ),
  });
}

/** Opens a form dialog for the modify deactivation action, pre-filled with the existing validity period bounds. */
function modifyDeactivation() {
  openFormDialog({
    i18nScope: 'AccountDeactivationActions.FormDialog.modify',
    formFields: accountLifecycleUiConfiguration['deactivation.modify'],
    initialFormData: accountStatus.value
      ? toAccountStatusForm(accountStatus.value)
      : undefined,
    onSubmit: (formData: AccountStatusForm) =>
      updateAccountStatus(
        () =>
          deactivateAccount(
            accountId.value,
            toAccountDeactivationRecord(formData)
          ),
        'modifyDeactivationSuccess',
        formData.validityPeriodEnd
      ),
  });
}

/** Opens a form dialog for the scheduled suspension action. */
function scheduledSuspension() {
  openFormDialog({
    i18nScope: 'AccountSuspensionActions.FormDialog.scheduled',
    formFields: accountLifecycleUiConfiguration['suspension.scheduled'],
    onSubmit: (formData: AccountStatusForm) =>
      updateAccountStatus(
        () =>
          suspendAccount(accountId.value, toAccountSuspensionRecord(formData)),
        'scheduledSuspensionSuccess',
        formData.suspensionPeriodStart
      ),
  });
}

/** Opens a form dialog for the modify suspension action, pre-filled with the existing suspension period bounds. */
function modifySuspension() {
  openFormDialog({
    i18nScope: 'AccountSuspensionActions.FormDialog.modify',
    formFields: accountLifecycleUiConfiguration['suspension.modify'],
    initialFormData: accountStatus.value
      ? toAccountStatusForm(accountStatus.value)
      : undefined,
    onSubmit: (formData: AccountStatusForm) =>
      updateAccountStatus(
        () =>
          suspendAccount(accountId.value, toAccountSuspensionRecord(formData)),
        'modifySuspensionSuccess',
        formData.suspensionPeriodStart
      ),
  });
}

/**
 * Runs a status-update API call, then updates the page state with the refreshed account information. Displays a
 * notification in case of an error during the update process.
 *
 * @param statusUpdate - The status-mutation service call to execute, resolving to the updated account DTO.
 * @param successMsgKey - Optional i18n key for the success message to display upon successful update.
 * @param dateToDisplayInSuccessMsg - Optional date to display in the success message.
 * @returns A promise that resolves once the account status has been updated and the page state has been refreshed with
 *   the new account information.
 */
async function updateAccountStatus(
  statusUpdate: () => Promise<AccountDTO>,
  successMsgKey = 'updateStatusSuccess',
  dateToDisplayInSuccessMsg?: string | null
): Promise<void> {
  isLoading.value = true;

  try {
    const dto = await statusUpdate();
    account.value = toAccount(dto);
    accountStatus.value = toAccountStatus(dto);

    Notify({
      type: 'positive',
      message: dateToDisplayInSuccessMsg
        ? t(successMsgKey, { date: dateToDisplayInSuccessMsg })
        : t(successMsgKey, { count: actionDelay }),
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
