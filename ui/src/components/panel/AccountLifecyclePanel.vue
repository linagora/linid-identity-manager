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
  <div
    v-if="lifecycleUi && accountStatus"
    class="column q-gutter-y-sm q-mb-md account-lifecycle-panel"
    data-cy="account-lifecycle-panel"
  >
    <component
      :is="lifecycleBannersCard"
      v-if="lifecycleBannersCard"
      :ui-namespace="uiNamespace"
      :i18n-scope="i18nScope"
      :entity="entity"
      :banners="displayedBanners"
      :emit-on-click="ACCOUNT_LIFECYCLE_ACTION_EVENT"
    />

    <AccountDeactivatedInfoText
      v-if="lifecycleUi.showWillDeactivateInfoText"
      :account-status="accountStatus"
    />

    <AccountSuspendedInfoText
      v-if="lifecycleUi.showWillSuspendInfoText"
      :account-status="accountStatus"
    />

    <AccountNotActivatedInfoText v-if="lifecycleUi.showNotActivatedInfoText" />

    <component
      :is="lifecycleButtonsCard"
      v-if="lifecycleButtonsCard"
      :ui-namespace="uiNamespace"
      :i18n-scope="i18nScope"
      :entity="entity"
      :items="lifecycleUi.menuItems ?? {}"
      :buttons="buttons"
      :actions="actions"
      :immediate-action-delay="immediateActionDelay"
      :emit-on-submit="ACCOUNT_STATUS_UPDATED_EVENT"
      :trigger-action-on="ACCOUNT_LIFECYCLE_ACTION_EVENT"
    />
  </div>
  <!-- v8 ignore stop -->
</template>

<script setup lang="ts">
import { loadAsyncComponent } from '@linagora/lin-pulsar-core';
import AccountDeactivatedInfoText from 'src/components/text/AccountDeactivatedInfoText.vue';
import AccountNotActivatedInfoText from 'src/components/text/AccountNotActivatedInfoText.vue';
import AccountSuspendedInfoText from 'src/components/text/AccountSuspendedInfoText.vue';
import { useAccountLifecycleUi } from 'src/composables/useAccountLifecycleUi';
import { useAccountMapper } from 'src/composables/useAccountMapper';
import type { AccountLifecyclePanelProps } from 'src/types/accountLifecyclePanelProps';
import type { AccountDTO, AccountStatus } from 'src/types/accounts';
import { computed } from 'vue';

defineOptions({ inheritAttrs: false });

const props = defineProps<AccountLifecyclePanelProps>();

/** UI event key notifying the hosting details page that the account status changed and the entity must be reloaded. */
const ACCOUNT_STATUS_UPDATED_EVENT = 'account-status-updated';

/** UI event key published by the lifecycle banners card to run one of the actions of the lifecycle buttons card. */
const ACCOUNT_LIFECYCLE_ACTION_EVENT = 'account-lifecycle-action';

const i18nScope = 'AccountLifecyclePanel';

const { toAccountStatus } = useAccountMapper();

const accountStatus = computed<AccountStatus | null>(() =>
  props.entity.id
    ? toAccountStatus(props.entity as unknown as AccountDTO)
    : null
);

const lifecycleUi = useAccountLifecycleUi(accountStatus);

/** Configured banners matching the account status, in configuration order. */
const displayedBanners = computed(() =>
  (props.banners ?? []).filter(({ key }) =>
    lifecycleUi.value?.banners?.includes(key)
  )
);

const lifecycleBannersCard = loadAsyncComponent(
  'LinPulsar/LifecycleBannersCard'
);

const lifecycleButtonsCard = loadAsyncComponent(
  'LinPulsar/LifecycleButtonsCard'
);
</script>
