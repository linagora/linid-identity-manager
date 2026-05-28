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
  <q-banner
    class="bg-warning text-white organizational-unit-suspended-banner"
    v-bind="uiProps.banner"
    data-cy="organizational-unit-suspended-banner"
  >
    <template
      v-if="uiProps.icon.name"
      #avatar
    >
      <q-icon v-bind="uiProps.icon" />
    </template>

    {{ t(contentI18nKey, { date: startDate, endDate }) }}

    <template #action>
      <q-btn
        :label="t('clearSuspensionButton')"
        v-bind="uiProps.clearSuspensionButton"
        data-cy="organizational-unit-suspended-banner_clear-suspension-button"
        @click="emit('clear-suspension')"
      />
      <q-btn
        :label="t('modifySuspensionEndButton')"
        v-bind="uiProps.modifySuspensionEndButton"
        data-cy="organizational-unit-suspended-banner_modify-suspension-end-button"
        @click="emit('modify-suspension-end')"
      />
    </template>
  </q-banner>
</template>

<script lang="ts" setup>
import type {
  LinidQBannerProps,
  LinidQBtnProps,
  LinidQIconProps,
} from '@linagora/linid-im-front-corelib';
import { useScopedI18n, useUiDesign } from '@linagora/linid-im-front-corelib';
import { useCommonMapper } from 'src/composables/useCommonMapper';
import type {
  OrganizationalUnitBannerProps,
  OrganizationalUnitSuspendedBannerOutputs,
} from 'src/types/organizationalUnitBannerProps';
import { computed } from 'vue';

const props = defineProps<OrganizationalUnitBannerProps>();
const emit = defineEmits<OrganizationalUnitSuspendedBannerOutputs>();

const localUiNamespace = `organizational-unit-suspended-banner`;
const localI18n = `OrganizationalUnitSuspendedBanner`;

const { t } = useScopedI18n(localI18n);
const { ui } = useUiDesign();
const { toDate } = useCommonMapper();

const startDate = computed(() =>
  toDate(props.organizationalUnitStatus.suspensionPeriod?.start)
);
const endDate = computed(() =>
  toDate(props.organizationalUnitStatus.suspensionPeriod?.end)
);
const contentI18nKey = computed(() =>
  props.organizationalUnitStatus.suspensionPeriod?.end != null
    ? 'contentWithEndDate'
    : 'content'
);

const uiProps = {
  banner: ui<LinidQBannerProps>(localUiNamespace, 'q-banner'),
  icon: ui<LinidQIconProps>(localUiNamespace, 'q-icon'),
  clearSuspensionButton: ui<LinidQBtnProps>(
    `${localUiNamespace}.clear-suspension-button`,
    'q-btn'
  ),
  modifySuspensionEndButton: ui<LinidQBtnProps>(
    `${localUiNamespace}.modify-suspension-end-button`,
    'q-btn'
  ),
};
</script>

<style scoped></style>
