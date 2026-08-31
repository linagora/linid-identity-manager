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
  <q-btn
    v-bind="uiProps.btn"
    :label="t('createChild')"
    :disable="!entity?.id"
    class="organizational-unit-create-child-button"
    data-cy="organizational-unit-create-child-button"
    @click="goToCreateChild"
  />
  <!-- v8 ignore stop -->
</template>

<script setup lang="ts">
import {
  type LinidQBtnProps,
  useScopedI18n,
  useUiDesign,
} from '@linagora/linid-im-front-corelib';
import type { OrganizationalUnitCreateChildBtnProps } from 'src/types/organizationalUnitCreateChildBtnProps';
import { computed } from 'vue';
import { useRouter } from 'vue-router';

defineOptions({ inheritAttrs: false });

const props = defineProps<OrganizationalUnitCreateChildBtnProps>();

const router = useRouter();
const { t } = useScopedI18n(props.i18nScope);
const { ui } = useUiDesign();

const uiProps = computed(() => ({
  btn: ui<LinidQBtnProps>(`${props.uiNamespace}.create-child-button`, 'q-btn'),
}));

/**
 * Navigates to the creation page to create a child organizational unit under the current one, which is passed as the
 * parent through the route query.
 */
function goToCreateChild(): void {
  void router.push({
    path: '/organizational-units/create',
    query: { parent: props.entity.id as string },
  });
}
</script>
