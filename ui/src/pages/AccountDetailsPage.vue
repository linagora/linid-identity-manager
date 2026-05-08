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
        <h1
          class="q-ma-none text-h5 account-details-page--title"
          data-cy="account-details-page_title"
        >
          {{ t('title') }}
        </h1>
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
        <AccountStatusBadge
          v-if="lifecycleUi.showBadge"
          :status="accountStatus.status"
        />

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

        <component
          :is="dropdownButton"
          v-if="dropdownButton && lifecycleUi.menuItems?.length"
          :ui-namespace="`${uiNamespace}.lifecycle-actions`"
          i18n-scope="AccountLifecycleActions"
          :items="lifecycleUi.menuItems"
          class="account-details-page--lifecycle--actions"
          data-cy="account-lifecycle-actions"
          @item-click="onDropdownItemClick"
        />
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
import { fieldsOrder } from 'src/assets/accounts/detailsConfiguration';
import AccountDeactivatedInfoText from 'src/components/AccountDeactivatedInfoText.vue';
import AccountNotActivatedInfoText from 'src/components/AccountNotActivatedInfoText.vue';
import AccountStatusBadge from 'src/components/AccountStatusBadge.vue';
import AccountSuspendedInfoText from 'src/components/AccountSuspendedInfoText.vue';
import AccountDeactivatedWarningBanner from 'src/components/banner/AccountDeactivatedWarningBanner.vue';
import AccountSuspendedBanner from 'src/components/banner/AccountSuspendedBanner.vue';
import { useAccountLifecycleUi } from 'src/composables/useAccountLifecycleUi';
import { useAccountMapper } from 'src/mappers/accountMapper';
import { getAccountById } from 'src/services/AccountService';
import type { AccountLifecycleAction } from 'src/types/accountLifecycleUi';
import { ACCOUNT_LIFECYCLE_ACTIONS } from 'src/types/accountLifecycleUi';
import { type Account, type AccountStatus } from 'src/types/accounts';
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const pageName = 'AccountDetailsPage';
const i18nScope = pageName;
const uiNamespace = 'accounts.details-page';

const route = useRoute();
const router = useRouter();
const { t } = useScopedI18n(i18nScope);
const { Notify } = useNotify();
const { toAccount, toAccountStatus } = useAccountMapper();

const accountId = computed(() => route.params.id as string);

const account = ref<Account | null>(null);
const accountStatus = ref<AccountStatus | null>(null);
const isLoading = ref<boolean>(false);

const entityDetailsCard = loadAsyncComponent('catalogUI/EntityDetailsCard');
const buttonsCard = loadAsyncComponent('catalogUI/ButtonsCard');
const dropdownButton = loadAsyncComponent('catalogUI/DropdownButton');

const lifecycleUi = useAccountLifecycleUi(accountStatus);

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

/**
 * Handles a lifecycle action triggered from a banner or the actions dropdown.
 * Currently a no-op placeholder; the dialogs that fulfil these actions are
 * delivered in a follow-up.
 * @param action - The triggered lifecycle action.
 */
function onLifecycleAction(action: AccountLifecycleAction): void {
  void action;
}

/**
 * Forwards a DropdownButton click as a typed lifecycle action when the key is
 * recognised; otherwise the event is ignored.
 * @param payload - The DropdownButton click payload.
 */
function onDropdownItemClick(payload: DropdownClickPayload): void {
  if (
    (ACCOUNT_LIFECYCLE_ACTIONS as ReadonlyArray<string>).includes(payload.key)
  ) {
    onLifecycleAction(payload.key as AccountLifecycleAction);
  }
}

onMounted(() => {
  loadAccount();
});
</script>
