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
    class="row justify-center q-pa-md organizational-unit-creation-page"
    data-cy="organizational-unit-creation-page"
  >
    <div
      class="col-12 col-md-8 col-lg-6 organizational-unit-creation-page--content"
    >
      <h1
        class="q-ma-none q-mb-md text-h5 organizational-unit-creation-page--title"
        data-cy="organizational-unit-creation-page_title"
      >
        {{ t('title') }}
      </h1>

      <q-form
        class="organizational-unit-creation-page--form"
        data-cy="organizational-unit-creation-page_form"
        @submit="onSubmit"
      >
        <div data-cy="field_parent">
          <q-input
            :model-value="parentName"
            :label="t('fields.parent')"
            readonly
            class="q-mb-sm"
            v-bind="uiProps.parent?.input"
            bottom-slots
          />
        </div>

        <div
          v-for="field in creationFields"
          :key="field.name"
          :data-cy="`field_${field.name}`"
        >
          <q-select
            v-if="field.type === 'select'"
            v-model="form[field.name]"
            :label="field.label"
            :options="field.options"
            :rules="field.rules"
            class="q-mb-sm"
            lazy-rules
            :data-cy="`field_${field.name}_select`"
            v-bind="uiProps[field.name]?.select"
          />

          <q-input
            v-else-if="field.type === 'text' || field.type === 'email'"
            v-model="form[field.name]"
            :label="field.label"
            :rules="field.rules"
            :type="field.type"
            class="q-mb-sm"
            lazy-rules
            v-bind="uiProps[field.name]?.input"
          />
        </div>

        <component
          :is="buttonsCard"
          v-if="buttonsCard"
          :ui-namespace="uiNamespace"
          :i18n-scope="i18nScope"
          :is-loading="isLoading"
          confirm-btn-type="submit"
          @cancel="cancel"
        />
      </q-form>
    </div>
  </q-page>
  <!-- v8 ignore stop -->
</template>

<script setup lang="ts">
import type {
  LinidQInputProps,
  LinidQSelectProps,
} from '@linagora/linid-im-front-corelib';
import {
  loadAsyncComponent,
  useNotify,
  useScopedI18n,
  useUiDesign,
} from '@linagora/linid-im-front-corelib';
import axios from 'axios';
import { useCommonMapper } from 'src/composables/useCommonMapper';
import { useOrganizationalUnitCreationConfig } from 'src/composables/useOrganizationalUnitCreationConfig';
import { useOrganizationalUnitMapper } from 'src/composables/useOrganizationalUnitMapper';
import {
  createOrganizationalUnit,
  getOrganizationalUnitById,
} from 'src/services/OrganizationalUnitService';
import type { FieldUiProps } from 'src/types/form';
import type { OrganizationalUnitForm } from 'src/types/organizationalUnits';
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const pageName = 'OrganizationalUnitCreationPage';
const i18nScope = pageName;
const uiNamespace = 'organizational-units.creation-page';

const route = useRoute();
const router = useRouter();
const { t } = useScopedI18n(i18nScope);
const { Notify } = useNotify();
const { toEmptyRecord } = useCommonMapper();
const { creationFields } = useOrganizationalUnitCreationConfig(i18nScope);
const { toOrganizationalUnitRecord } = useOrganizationalUnitMapper();
const { ui } = useUiDesign();

const form = reactive<OrganizationalUnitForm>(toEmptyRecord(creationFields));
const isLoading = ref<boolean>(false);
const parentId = ref<string>('');
const parentName = ref<string>('');

const buttonsCard = loadAsyncComponent('catalogUI/ButtonsCard');

const uiProps = [
  'parent',
  ...creationFields.map((field) => field.name),
].reduce<FieldUiProps>((acc, name) => {
  acc[name] = {
    input: ui<LinidQInputProps>(`${uiNamespace}.fields.${name}`, 'q-input'),
    select: ui<LinidQSelectProps>(`${uiNamespace}.fields.${name}`, 'q-select'),
  };
  return acc;
}, {});

/**
 * Resolves the parent OU from the route query, fetches its name, and stores both for the lifetime of the page.
 * Redirects to the home page when the parent is missing or cannot be loaded: creating an OU without a parent is not
 * allowed.
 */
async function loadParent(): Promise<void> {
  const rawParent = route.query.parent;
  const id = typeof rawParent === 'string' ? rawParent : '';
  if (!id) {
    Notify({
      type: 'negative',
      message: t('errors.missingParent'),
    });
    void router.push('/');
    return;
  }
  try {
    const parent = await getOrganizationalUnitById(id);
    parentId.value = parent.id;
    parentName.value = parent.name;
  } catch {
    Notify({
      type: 'negative',
      message: t('errors.missingParent'),
    });
    void router.push('/');
  }
}

/**
 * Submits the form by creating a new organizational unit under the resolved parent, then redirects to the parent OU
 * details page.
 *
 * Called by q-form only when the enclosing form validation succeeds, so no extra client-side validation is required
 * here.
 *
 * @returns A promise that resolves when the creation process is complete.
 */
async function onSubmit(): Promise<void> {
  isLoading.value = true;
  try {
    await createOrganizationalUnit(
      toOrganizationalUnitRecord({ ...form }, parentId.value)
    );
    Notify({
      type: 'positive',
      message: t('success'),
    });
    void router.push({
      path: '/organizational-units',
      query: { node: parentId.value },
    });
  } catch (error) {
    const errorMessageKey =
      axios.isAxiosError(error) && error.response?.status === 400
        ? 'errors.validation'
        : 'errors.generic';
    Notify({
      type: 'negative',
      message: t(errorMessageKey),
    });
  } finally {
    isLoading.value = false;
  }
}

/** Cancels the OU creation and navigates back to the parent OU details. */
function cancel(): void {
  void router.push({
    path: '/organizational-units',
    query: { node: parentId.value },
  });
}

onMounted(() => {
  void loadParent();
});
</script>
