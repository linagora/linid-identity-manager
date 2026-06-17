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
    class="row justify-center q-pa-md account-creation-page"
    data-cy="account-creation-page"
  >
    <div class="col-12 col-md-8 col-lg-6 account-creation-page--content">
      <h1
        class="q-ma-none q-mb-md text-h5 account-creation-page--title"
        data-cy="account-creation-page_title"
      >
        {{ t('title') }}
      </h1>

      <q-form
        class="account-creation-page--form"
        data-cy="account-creation-page_form"
        @submit="onSubmit"
      >
        <div
          v-for="field in creationFields"
          :key="field.name"
          :data-cy="`field_${field.name}`"
        >
          <component
            :is="fieldComponent"
            v-if="fieldComponent"
            v-model:entity="form"
            :ui-namespace="`${uiNamespace}.fields`"
            :i18n-scope="i18nScope"
            :definition="field"
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
import {
  loadAsyncComponent,
  useNotify,
  useScopedI18n,
} from '@linagora/linid-im-front-corelib';
import axios from 'axios';
import { appConfig } from 'src/boot/config';
import { useAccountMapper } from 'src/composables/useAccountMapper';
import { useCommonMapper } from 'src/composables/useCommonMapper';
import { createAccount } from 'src/services/AccountService';
import {
  getOrganizationalUnitById,
  getOrganizationalUnitRoot,
} from 'src/services/OrganizationalUnitService';
import type { AccountForm } from 'src/types/accounts';
import type { OrganizationalUnitDTO } from 'src/types/organizationalUnits';
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const pageName = 'AccountCreationPage';
const i18nScope = pageName;
const uiNamespace = 'accounts.creation-page';

const router = useRouter();
const { t } = useScopedI18n(i18nScope);
const { Notify } = useNotify();
const { toAccountRecord } = useAccountMapper();
const { toEmptyRecord } = useCommonMapper();
const creationFields = appConfig.accountCreationFields;

const fieldComponent = loadAsyncComponent('catalogUI/EntityAttributeField');
const buttonsCard = loadAsyncComponent('catalogUI/ButtonsCard');

const form = ref<AccountForm>(toEmptyRecord<AccountForm>(creationFields));
const isLoading = ref<boolean>(false);
const route = useRoute();
const ouSelected: string = route.query.ou as string;
const organizationalUnit = ref<OrganizationalUnitDTO>();

/**
 * Loads the organizational unit based on the route query parameter.
 *
 * @returns A promise that resolves when the organizational unit is loaded, or redirects to the accounts list if not
 *   found or on error.
 */
async function loadOrganizationalUnit() {
  const promise = ouSelected
    ? getOrganizationalUnitById(ouSelected)
    : getOrganizationalUnitRoot();

  return promise
    .then((ou) => {
      organizationalUnit.value = ou;
    })
    .catch((error) => {
      const errorMessageKey =
        axios.isAxiosError(error) && error.response?.status === 404
          ? 'errors.notFound'
          : 'errors.generic';

      Notify({
        type: 'negative',
        message: t(errorMessageKey),
      });

      router.push(`/accounts`);

      return Promise.reject(error);
    });
}

/** Sets the organizational unit name in the form. */
function setFormData() {
  form.value = {
    ...form.value,
    organizationalUnit: organizationalUnit.value!.name,
  };
}

/**
 * Submits the form by creating a new account, then redirects to its details page.
 *
 * Called by q-form only when the enclosing form validation succeeds, so no extra client-side validation is required
 * here.
 *
 * @returns A promise that resolves when the creation process is complete.
 */
async function onSubmit(): Promise<void> {
  isLoading.value = true;
  try {
    const created = await createAccount(
      toAccountRecord({ ...form.value }, organizationalUnit.value!.id)
    );
    Notify({
      type: 'positive',
      message: t('success'),
    });
    router.push(`/accounts/${created.id}`);
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

/** Cancels the account creation and navigates back to the accounts list. */
function cancel(): void {
  const query = { node: '' };
  if (route.query.node) {
    query.node = route.query.node as string;
  }
  void router.push({ path: '/accounts', query });
}

onMounted(async () => {
  loadOrganizationalUnit()
    .then(setFormData)
    .catch(() => {
      // Error is already handled by loadOrganizationalUnit
    });
});
</script>
