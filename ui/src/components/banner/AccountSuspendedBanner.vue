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
    class="bg-warning text-white account-suspended-banner"
    v-bind="uiProps.banner"
    data-cy="account-suspended-banner"
  >
    <template
      v-if="uiProps.icon.name"
      #avatar
    >
      <q-icon v-bind="uiProps.icon" />
    </template>

    {{ t(contentI18nKey, { date, endDate }) }}

    <template #action>
      <q-btn
        :label="t('clearSuspensionButton')"
        v-bind="uiProps.clearSuspensionButton"
        @click="emit('clear-suspension')"
      />
      <q-btn
        :label="t('modifySuspensionButton')"
        v-bind="uiProps.modifySuspensionButton"
        @click="emit('modify-suspension')"
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
import { useCommonMapper } from 'src/mappers/commonMapper';
import type {
  AccountBannerProps,
  AccountSuspendedBannerOutputs,
} from 'src/types/accountBannerProps';
import { computed } from 'vue';

const props = defineProps<AccountBannerProps>();
const emit = defineEmits<AccountSuspendedBannerOutputs>();

const localUiNamespace = `account-suspended-banner`;
const localI18n = `AccountSuspendedBanner`;

const { t } = useScopedI18n(localI18n);
const { ui } = useUiDesign();
const { toDate } = useCommonMapper();

const date = computed(() => toDate(props.accountStatus.suspensionPeriod.start));
const endDate = computed(() =>
  toDate(props.accountStatus.suspensionPeriod.end)
);
const contentI18nKey = computed(() =>
  props.accountStatus.suspensionPeriod.end != null
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
  modifySuspensionButton: ui<LinidQBtnProps>(
    `${localUiNamespace}.modify-suspension-button`,
    'q-btn'
  ),
};
</script>

<style scoped></style>
