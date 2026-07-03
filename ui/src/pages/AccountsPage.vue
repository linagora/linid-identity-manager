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
  <q-page
    data-cy="accounts-page"
    class="q-pa-md accounts-page"
  >
    <div class="accounts-page--header">
      <h1
        class="q-ma-none text-h5 accounts-page--title"
        data-cy="accounts-page_title"
      >
        {{ t('title') }}
      </h1>
    </div>
    <component
      :is="advancedSearchComponent"
      v-if="advancedSearchComponent"
      :filters="filters"
      :ui-namespace="uiNamespace"
      :i18n-scope="i18nScope"
      :fields="fieldsSearch"
      :default-fields-names="defaultFields"
      :advanced-fields-names="advancedFields"
      class="q-mb-md"
      @update:filters="onFiltersChange"
    />
    <component
      :is="tableComponent"
      v-if="tableComponent && accounts.length > 0"
      v-model:pagination="pagination"
      :ui-namespace="uiNamespace"
      :i18n-scope="i18nScope"
      :rows="accounts"
      :columns="accountColumns"
      :loading="isLoading"
      @request="onRequest"
    >
      <template #body="props">
        <q-tr
          :props="props"
          data-cy="account-row"
        >
          <q-td
            v-for="col in props.cols"
            :key="col.name"
            :props="props"
            :data-cy="`cell-${col.name}`"
          >
            <template v-if="col.name === 'actions'">
              <q-btn
                v-bind="uiProps.seeButton"
                :label="t('detailButton')"
                data-cy="see-button"
                @click="goToAccountDetails(props.row)"
              />
            </template>
            <template v-else>
              {{ col.value }}
            </template>
          </q-td>
        </q-tr>
      </template>
    </component>
    <span v-else>
      {{ t('noAccount') }}
    </span>
  </q-page>
</template>

<script setup lang="ts">
import type {
  LinidQBtnProps,
  QTableRequestEvent,
  QuasarPagination,
} from '@linagora/linid-im-front-corelib';
import {
  loadAsyncComponent,
  useNotify,
  usePagination,
  useScopedI18n,
  useUiDesign,
} from '@linagora/linid-im-front-corelib';
import axios from 'axios';
import { storeToRefs } from 'pinia';
import { appConfig } from 'src/boot/config';
import { useAccountMapper } from 'src/composables/useAccountMapper';
import { getAccountsByOrganizationalUnitId } from 'src/services/OrganizationalUnitService';
import { useOrganizationalUnitStore } from 'src/stores/useOrganizationalUnitStore';
import type { Account } from 'src/types/accounts';
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const store = useOrganizationalUnitStore();
const { selectedOrganizationalUnitId } = storeToRefs(store);
const fieldsSearch = appConfig.accountSearchFields;
const defaultFields = appConfig.accountSearchDefaultFields;
const advancedFields = appConfig.accountSearchAdvancedFields;

const router = useRouter();
const route = useRoute();
const i18nScope = 'AccountsPage';
const { t } = useScopedI18n(i18nScope);
const { Notify } = useNotify();
const advancedSearchComponent = loadAsyncComponent(
  'catalogUI/AdvancedSearchCard'
);
const tableComponent = loadAsyncComponent('catalogUI/GenericEntityTable');
const filters = ref<Record<string, unknown>>({});

const accounts = ref<Account[]>([]);
const isLoading = ref<boolean>(false);

let listRequestController: AbortController | null = null;

const { ui } = useUiDesign();
const uiProps = computed(() => ({
  seeButton: ui<LinidQBtnProps>(
    `${uiNamespace}.buttons-card.see-button`,
    'q-btn'
  ),
}));

const { toAccountQueryFilterDTO, toAccountList } = useAccountMapper();
const { toPagination, toQuasarPagination } = usePagination();
const accountColumns = computed(() =>
  appConfig.accountTableColumns.map((col) => ({
    ...col,
    label: t(`accountColumns.${col.name}`),
  }))
);

const pagination = ref<QuasarPagination>({
  page: 1,
  rowsNumber: 0,
  sortBy: undefined,
  rowsPerPage: 10,
  descending: true,
});
const uiNamespace = 'accounts.homepage';

watch(
  selectedOrganizationalUnitId,
  (nodeId: string) => {
    if (!nodeId) {
      return;
    }
    pagination.value.page = 1;
    filters.value = {};
    loadData(nodeId);
  },
  { immediate: true }
);

/**
 * Loads the account list.
 *
 * @param id - Organizational unit ID to filter accounts, if empty load all accounts related to root node.
 */
async function loadData(id: string): Promise<void> {
  listRequestController?.abort();
  const controller = new AbortController();
  listRequestController = controller;

  isLoading.value = true;

  try {
    const accountsPage = await getAccountsByOrganizationalUnitId(
      id,
      toAccountQueryFilterDTO(filters.value),
      toPagination(pagination.value),
      controller.signal
    );
    accounts.value = toAccountList(accountsPage.content);
    pagination.value = toQuasarPagination(accountsPage);
  } catch (error) {
    if (axios.isCancel(error)) {
      return;
    }

    const errorMessageKey =
      axios.isAxiosError(error) && error.response?.status === 404
        ? 'errors.notFound'
        : 'errors.generic';
    Notify({
      type: 'negative',
      message: t(errorMessageKey),
    });
  } finally {
    if (listRequestController === controller) {
      isLoading.value = false;
    }
  }
}

/**
 * Navigates to the account detail page.
 *
 * @param account - Account row selected in the table.
 * @returns A promise resolved once navigation finishes.
 */
function goToAccountDetails(account: Account) {
  return router.push({
    path: `${route.path}/${account.id}`,
  });
}

/**
 * Handles pagination/sort changes requested by QTable.
 *
 * @param props - QTable request payload.
 */
async function onRequest(props: QTableRequestEvent) {
  pagination.value = props.pagination;
  await loadData(selectedOrganizationalUnitId.value);
}

/**
 * Handles filter changes from the AdvancedSearchCard component. Resets pagination to the first page and reloads data
 * with new filters.
 *
 * @param newFilters - The updated filters object.
 * @returns A promise that resolves when the data has been loaded.
 */
function onFiltersChange(newFilters: Record<string, unknown>): Promise<void> {
  filters.value = newFilters;
  pagination.value.page = 1;
  return loadData(selectedOrganizationalUnitId.value);
}
</script>
