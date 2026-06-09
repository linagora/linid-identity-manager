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
    class="row justify-center q-pa-md organizational-unit-details-page"
    data-cy="organizational-unit-details-page"
  >
    <div
      class="col-12 col-md-10 col-lg-10 organizational-unit-details-page--content"
    >
      <div
        class="row items-center justify-between q-mb-md organizational-unit-details-page--header"
      >
        <div class="row items-center q-gutter-x-md">
          <h1
            class="q-ma-none text-h5 organizational-unit-details-page--title"
            data-cy="organizational-unit-details-page_title"
          >
            {{ t('title') }}
          </h1>
          <StatusBadge
            v-if="lifecycleUi?.showBadge && organizationalUnitStatus"
            :status="
              organizationalUnitStatus.isSuspended ? 'SUSPENDED' : 'ACTIVE'
            "
          />
        </div>
      </div>

      <div
        v-if="lifecycleUi && organizationalUnitStatus"
        class="column q-gutter-y-sm q-mb-md organizational-unit-details-page--lifecycle"
        data-cy="organizational-unit-details-page_lifecycle"
      >
        <OrganizationalUnitSuspendedBanner
          v-if="lifecycleUi.showSuspendedBanner"
          :organizational-unit-status="organizationalUnitStatus"
          @clear-suspension="onClearSuspension"
          @modify-suspension-end="onModifySuspensionEnd"
        />

        <OrganizationalUnitSuspendedInfoText
          v-if="lifecycleUi.showWillSuspendInfoText"
          :organizational-unit-status="organizationalUnitStatus"
        />

        <div
          v-if="hasAnyLifecycleAction"
          class="row q-gutter-x-sm organizational-unit-details-page--lifecycle--actions"
          data-cy="organizational-unit-lifecycle-actions"
        >
          <component
            :is="dropdownButton"
            v-if="dropdownButton && lifecycleUi.activationMenuItems?.length"
            :ui-namespace="`${uiNamespace}.activation-actions`"
            i18n-scope="OrganizationalUnitActivationActions"
            :items="lifecycleUi.activationMenuItems"
            data-cy="organizational-unit-activation-actions"
            @item-click="onLifecycleActionClick"
          />
          <component
            :is="dropdownButton"
            v-if="dropdownButton && lifecycleUi.suspensionMenuItems?.length"
            :ui-namespace="`${uiNamespace}.suspension-actions`"
            i18n-scope="OrganizationalUnitSuspensionActions"
            :items="lifecycleUi.suspensionMenuItems"
            data-cy="organizational-unit-suspension-actions"
            @item-click="onLifecycleActionClick"
          />
        </div>
      </div>

      <component
        :is="entityDetailsCard"
        v-if="entityDetailsCard"
        :entity="organizationalUnit ?? {}"
        :field-order="fieldsOrder"
        :is-loading="isLoading"
        :ui-namespace="uiNamespace"
        :i18n-scope="i18nScope"
        class="q-mb-md organizational-unit-details-page--cards"
        data-cy="organizational-unit-details-page_cards"
      />
    </div>
  </q-page>
  <!-- v8 ignore stop -->
</template>

<script setup lang="ts">
import {
  type DropdownClickPayload,
  loadAsyncComponent,
  useNotify,
  useScopedI18n,
} from '@linagora/linid-im-front-corelib';
import axios from 'axios';
import { storeToRefs } from 'pinia';
import { fieldsOrder } from 'src/assets/organizationalUnits/detailsConfiguration';
import { organizationalUnitLifecycleUiConfiguration } from 'src/assets/organizationalUnits/organizationalUnitLifecycleUiConfiguration';
import { dayjs } from 'src/boot/dayjs';
import StatusBadge from 'src/components/badge/StatusBadge.vue';
import OrganizationalUnitSuspendedBanner from 'src/components/banner/OrganizationalUnitSuspendedBanner.vue';
import OrganizationalUnitSuspendedInfoText from 'src/components/text/OrganizationalUnitSuspendedInfoText.vue';
import { useLifecycleDialogs } from 'src/composables/useLifecycleDialogs';
import { useOrganizationalUnitLifecycleUi } from 'src/composables/useOrganizationalUnitLifecycleUi';
import { useOrganizationalUnitMapper } from 'src/composables/useOrganizationalUnitMapper';
import {
  getOrganizationalUnitById,
  reactivateOrganizationalUnit,
  suspendOrganizationalUnit,
} from 'src/services/OrganizationalUnitService';
import { useOrganizationalUnitStore } from 'src/stores/useOrganizationalUnitStore';
import type {
  OrganizationalUnit,
  OrganizationalUnitDTO,
  OrganizationalUnitStatus,
  OrganizationalUnitStatusForm,
} from 'src/types/organizationalUnits';
import { computed, onMounted, ref, watch } from 'vue';

const pageName = 'OrganizationalUnitDetailsPage';
const i18nScope = pageName;
const uiNamespace = 'organizational-units.details-page';

const { t } = useScopedI18n(i18nScope);
const { Notify } = useNotify();
const {
  toOrganizationalUnit,
  toOrganizationalUnitStatus,
  toOrganizationalUnitStatusForm,
  toOrganizationalUnitSuspensionRecord,
  toOrganizationalUnitReactivationRecord,
} = useOrganizationalUnitMapper();

const store = useOrganizationalUnitStore();
const { selectedOrganizationalUnitId } = storeToRefs(store);

const organizationalUnit = ref<OrganizationalUnit | null>(null);
const organizationalUnitStatus = ref<OrganizationalUnitStatus | null>(null);
const isLoading = ref<boolean>(false);

let detailRequestController: AbortController | null = null;

const entityDetailsCard = loadAsyncComponent('catalogUI/EntityDetailsCard');
const dropdownButton = loadAsyncComponent('catalogUI/DropdownButton');

const lifecycleUi = useOrganizationalUnitLifecycleUi(organizationalUnitStatus);

const { openFormDialog } = useLifecycleDialogs(uiNamespace);

const hasAnyLifecycleAction = computed(() =>
  Boolean(
    lifecycleUi.value?.activationMenuItems?.length ||
    lifecycleUi.value?.suspensionMenuItems?.length
  )
);

/**
 * Loads the organizational unit selected in the tree (store) and splits the
 * raw DTO into the identity and lifecycle projections. Clears the panel when
 * no organizational unit is selected.
 * @param id - Identifier of the organizational unit to load.
 */
async function loadOrganizationalUnit(id: string): Promise<void> {
  if (!id) {
    organizationalUnit.value = null;
    organizationalUnitStatus.value = null;
    return;
  }

  detailRequestController?.abort();
  const controller = new AbortController();
  detailRequestController = controller;

  isLoading.value = true;
  try {
    const dto = await getOrganizationalUnitById(id, controller.signal);
    organizationalUnit.value = toOrganizationalUnit(dto);
    organizationalUnitStatus.value = toOrganizationalUnitStatus(dto);
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
    if (detailRequestController === controller) {
      isLoading.value = false;
    }
  }
}

watch(selectedOrganizationalUnitId, (id: string) => {
  void loadOrganizationalUnit(id);
});

/**
 * Dispatches a lifecycle action key (emitted by the dropdown button) to the
 * matching dialog opening function.
 * @param event - Click event payload emitted by the dropdown button.
 * @param event.key - Dotted lifecycle action key to dispatch, for example
 *   "suspension.immediate".
 */
function onLifecycleActionClick(event: DropdownClickPayload): void {
  switch (event.key) {
    case 'suspension.immediate':
      openImmediateSuspensionDialog();
      break;
    case 'suspension.scheduled':
      openScheduleSuspensionDialog();
      break;
    case 'reactivation.immediate':
      onClearSuspension();
      break;
  }
}

/**
 * Opens the form dialog for an immediate suspension. Collects a reason,
 * sub-reason and optional comment, then submits a suspension period starting
 * one hour from now with no end date.
 */
function openImmediateSuspensionDialog(): void {
  openFormDialog({
    uiNamespace: `${uiNamespace}.suspend-dialog`,
    i18nScope: 'OrganizationalUnitSuspendDialog',
    formFields:
      organizationalUnitLifecycleUiConfiguration['suspension.immediate'],
    onSubmit: (formData: OrganizationalUnitStatusForm) =>
      submitStatus(
        () =>
          suspendOrganizationalUnit(
            selectedOrganizationalUnitId.value,
            toOrganizationalUnitSuspensionRecord({
              ...formData,
              start: dayjs().add(1, 'hour').toISOString(),
            })
          ),
        'success.suspended'
      ),
  });
}

/**
 * Opens the form dialog for scheduling a future suspension.
 */
function openScheduleSuspensionDialog(): void {
  openFormDialog({
    uiNamespace: `${uiNamespace}.schedule-suspension-dialog`,
    i18nScope: 'OrganizationalUnitScheduleSuspensionDialog',
    formFields:
      organizationalUnitLifecycleUiConfiguration['suspension.scheduled'],
    onSubmit: (formData: OrganizationalUnitStatusForm) =>
      submitStatus(
        () =>
          suspendOrganizationalUnit(
            selectedOrganizationalUnitId.value,
            toOrganizationalUnitSuspensionRecord(formData)
          ),
        'success.scheduled'
      ),
  });
}

/**
 * Opens the form dialog for reactivating a suspended organizational unit. The
 * backend lifts the suspension by setting its end to now.
 */
function onClearSuspension(): void {
  openFormDialog({
    uiNamespace: `${uiNamespace}.reactivate-dialog`,
    i18nScope: 'OrganizationalUnitReactivateDialog',
    formFields:
      organizationalUnitLifecycleUiConfiguration['reactivation.immediate'],
    onSubmit: (formData: OrganizationalUnitStatusForm) =>
      submitStatus(
        () =>
          reactivateOrganizationalUnit(
            selectedOrganizationalUnitId.value,
            toOrganizationalUnitReactivationRecord(formData)
          ),
        'success.reactivated'
      ),
  });
}

/**
 * Opens the form dialog for modifying the suspension end date while the OU is
 * currently suspended, pre-filled with the existing suspension period bounds.
 */
function onModifySuspensionEnd(): void {
  const currentStart =
    organizationalUnitStatus.value?.suspensionPeriod?.start ?? null;
  if (currentStart == null) {
    return;
  }

  openFormDialog({
    uiNamespace: `${uiNamespace}.edit-suspension-end-dialog`,
    i18nScope: 'OrganizationalUnitEditSuspensionEndDialog',
    formFields: organizationalUnitLifecycleUiConfiguration['suspension.modify'],
    initialFormData: organizationalUnitStatus.value
      ? toOrganizationalUnitStatusForm(organizationalUnitStatus.value)
      : undefined,
    onSubmit: (formData: OrganizationalUnitStatusForm) =>
      submitStatus(
        () =>
          suspendOrganizationalUnit(
            selectedOrganizationalUnitId.value,
            toOrganizationalUnitSuspensionRecord({
              ...formData,
              start: currentStart,
            })
          ),
        'success.endUpdated'
      ),
  });
}

/**
 * Runs a status-update API call and refreshes the local state. Surfaces a
 * positive notification on success and a negative one on failure.
 * @param statusUpdate - The status-mutation service call to execute, resolving to the updated OU DTO.
 * @param successKey - The i18n key used for the success notification.
 */
async function submitStatus(
  statusUpdate: () => Promise<OrganizationalUnitDTO>,
  successKey: string
): Promise<void> {
  isLoading.value = true;
  try {
    const dto = await statusUpdate();
    organizationalUnit.value = toOrganizationalUnit(dto);
    organizationalUnitStatus.value = toOrganizationalUnitStatus(dto);
    Notify({ type: 'positive', message: t(successKey as never) as string });
  } catch (error) {
    const errorMessageKey =
      axios.isAxiosError(error) && error.response?.status === 400
        ? 'errors.validation'
        : 'errors.generic';
    Notify({ type: 'negative', message: t(errorMessageKey) });
    throw error;
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => {
  void loadOrganizationalUnit(selectedOrganizationalUnitId.value);
});
</script>
