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
    v-if="lifecycleUi && organizationalUnitStatus"
    class="column q-gutter-y-sm q-mb-md organizational-unit-lifecycle-panel"
    data-cy="organizational-unit-lifecycle-panel"
  >
    <component
      :is="lifecycleBannersCard"
      v-if="lifecycleBannersCard"
      :ui-namespace="uiNamespace"
      :i18n-scope="i18nScope"
      :entity="entity"
      :banners="displayedBanners"
      :emit-on-click="ORGANIZATIONAL_UNIT_LIFECYCLE_ACTION_EVENT"
    />

    <OrganizationalUnitSuspendedInfoText
      v-if="lifecycleUi.showWillSuspendInfoText"
      :organizational-unit-status="organizationalUnitStatus"
    />

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
      :emit-on-submit="ORGANIZATIONAL_UNIT_STATUS_UPDATED_EVENT"
      :trigger-action-on="ORGANIZATIONAL_UNIT_LIFECYCLE_ACTION_EVENT"
    />
  </div>
  <!-- v8 ignore stop -->
</template>

<script setup lang="ts">
import { loadAsyncComponent } from '@linagora/lin-pulsar-core';
import OrganizationalUnitSuspendedInfoText from 'src/components/text/OrganizationalUnitSuspendedInfoText.vue';
import { useOrganizationalUnitLifecycleUi } from 'src/composables/useOrganizationalUnitLifecycleUi';
import { useOrganizationalUnitMapper } from 'src/composables/useOrganizationalUnitMapper';
import type { OrganizationalUnitLifecyclePanelProps } from 'src/types/organizationalUnitLifecyclePanelProps';
import type {
  OrganizationalUnitDTO,
  OrganizationalUnitStatus,
} from 'src/types/organizationalUnits';
import { computed } from 'vue';

defineOptions({ inheritAttrs: false });

const props = defineProps<OrganizationalUnitLifecyclePanelProps>();

/** UI event key notifying the hosting details page that the OU status changed and the entity must be reloaded. */
const ORGANIZATIONAL_UNIT_STATUS_UPDATED_EVENT =
  'organizational-unit-status-updated';

/** UI event key published by the lifecycle banners card to run one of the actions of the lifecycle buttons card. */
const ORGANIZATIONAL_UNIT_LIFECYCLE_ACTION_EVENT =
  'organizational-unit-lifecycle-action';

const i18nScope = 'OrganizationalUnitLifecyclePanel';

const { toOrganizationalUnitStatus } = useOrganizationalUnitMapper();

const organizationalUnitStatus = computed<OrganizationalUnitStatus | null>(
  () =>
    props.entity.id
      ? toOrganizationalUnitStatus(
          props.entity as unknown as OrganizationalUnitDTO
        )
      : null
);

const lifecycleUi = useOrganizationalUnitLifecycleUi(organizationalUnitStatus);

/** Configured banners matching the OU status, in configuration order. */
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
