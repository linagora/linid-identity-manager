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
  <div data-cy="field_parent">
    <q-input
      :model-value="parentName"
      :label="t('fields.parent')"
      readonly
      class="q-mb-sm"
      v-bind="parentUiInputProps"
      bottom-slots
    />
  </div>
  <!-- v8 ignore stop -->
</template>

<script setup lang="ts">
import type { LinidQInputProps } from '@linagora/linid-im-front-corelib';
import {
  uiEventSubject,
  useNotify,
  useNunjucks,
  useScopedI18n,
  useUiDesign,
} from '@linagora/linid-im-front-corelib';
import { getOrganizationalUnitById } from 'src/services/OrganizationalUnitService';
import { type OrganizationalUnitParentFieldProps } from 'src/types/organizationalUnitParentField';
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

defineOptions({ inheritAttrs: false });

const props = defineProps<OrganizationalUnitParentFieldProps>();

const route = useRoute();
const router = useRouter();
const { t } = useScopedI18n(props.i18nScope);
const { Notify } = useNotify();
const { ui } = useUiDesign();
const { renderString } = useNunjucks();

const parentName = ref<string>('');

const parentUiInputProps = ui<LinidQInputProps>(
  `${props.uiNamespace}.parent`,
  'q-input'
);

/**
 * Resolves the parent OU from the route query, fetches its name, and stores both for the lifetime of the page. Creating
 * an OU without a parent is not allowed, so on error the user is redirected:
 *
 * - If the parent query parameter is absent, to {@link OrganizationalUnitParentFieldProps.homepagePath};
 * - If the API call fails, to the parent OU detail page via {@link OrganizationalUnitParentFieldProps.parentPath} rendered
 *   with the known parent id.
 */
async function loadParent(): Promise<void> {
  const rawParent = route.query.parent;
  const id = typeof rawParent === 'string' ? rawParent : '';
  if (!id) {
    Notify({
      type: 'negative',
      message: t('missingParent'),
    });
    void router.push(props.homepagePath);
    return;
  }
  try {
    const parent = await getOrganizationalUnitById(id);
    parentName.value = parent.name;
    uiEventSubject.next({
      key: props.emitOnParentLoaded,
      data: { ...props.entity, parent: parent.id },
    });
  } catch {
    Notify({
      type: 'negative',
      message: t('errorLoadingParent'),
    });
    void router.push(renderString(props.parentPath, { parentId: id }));
  }
}

onMounted(() => {
  void loadParent();
});
</script>
