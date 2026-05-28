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
  getI18nInstance,
  loadAsyncComponent,
  uiEventSubject,
  useNotify,
  useScopedI18n,
} from '@linagora/linid-im-front-corelib';
import axios from 'axios';
import { fieldsOrder } from 'src/assets/organizationalUnits/detailsConfiguration';
import { organizationalUnitLifecycleUiConfiguration } from 'src/assets/organizationalUnits/organizationalUnitLifecycleUiConfiguration';
import { dayjs } from 'src/boot/dayjs';
import StatusBadge from 'src/components/badge/StatusBadge.vue';
import OrganizationalUnitSuspendedBanner from 'src/components/banner/OrganizationalUnitSuspendedBanner.vue';
import OrganizationalUnitSuspendedInfoText from 'src/components/text/OrganizationalUnitSuspendedInfoText.vue';
import { useOrganizationalUnitLifecycleUi } from 'src/composables/useOrganizationalUnitLifecycleUi';
import { useOrganizationalUnitMapper } from 'src/composables/useOrganizationalUnitMapper';
import {
  getOrganizationalUnitById,
  updateOrganizationalUnitStatus,
} from 'src/services/OrganizationalUnitService';
import type {
  OrganizationalUnit,
  OrganizationalUnitStatus,
  OrganizationalUnitStatusForm,
} from 'src/types/organizationalUnits';
import { computed, onMounted, ref } from 'vue';
import { type Composer } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

const pageName = 'OrganizationalUnitDetailsPage';
const i18nScope = pageName;
const uiNamespace = 'organizational-units.details-page';

const route = useRoute();
const router = useRouter();
const { t } = useScopedI18n(i18nScope);
const tGlobal = (getI18nInstance().global as Composer).t;
const { Notify } = useNotify();
const {
  toOrganizationalUnit,
  toOrganizationalUnitStatus,
  toOrganizationalUnitStatusForm,
  toOrganizationalUnitStatusRecord,
} = useOrganizationalUnitMapper();

const organizationalUnitId = computed(() => route.params.id as string);

const organizationalUnit = ref<OrganizationalUnit | null>(null);
const organizationalUnitStatus = ref<OrganizationalUnitStatus | null>(null);
const isLoading = ref<boolean>(false);

const entityDetailsCard = loadAsyncComponent('catalogUI/EntityDetailsCard');
const dropdownButton = loadAsyncComponent('catalogUI/DropdownButton');

const lifecycleUi = useOrganizationalUnitLifecycleUi(organizationalUnitStatus);

const hasAnyLifecycleAction = computed(() =>
  Boolean(
    lifecycleUi.value?.activationMenuItems?.length ||
    lifecycleUi.value?.suspensionMenuItems?.length
  )
);

/**
 * Loads the OU from the backend based on the route parameter and splits the
 * raw DTO into the page-level identity and lifecycle projections.
 */
async function loadOrganizationalUnit(): Promise<void> {
  isLoading.value = true;
  try {
    const dto = await getOrganizationalUnitById(organizationalUnitId.value);
    organizationalUnit.value = toOrganizationalUnit(dto);
    organizationalUnitStatus.value = toOrganizationalUnitStatus(dto);
  } catch (error) {
    const errorMessageKey =
      axios.isAxiosError(error) && error.response?.status === 404
        ? 'errors.notFound'
        : 'errors.generic';
    Notify({
      type: 'negative',
      message: t(errorMessageKey),
    });
    goHome();
  } finally {
    isLoading.value = false;
  }
}

/**
 * Navigates back to the home page.
 *
 * No OU listing page exists yet, so the home page acts as the safe fallback.
 */
function goHome(): void {
  void router.push('/');
}

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
 * Opens the confirmation dialog for an immediate suspension. Submits a
 * suspension period starting now with no end date.
 */
function openImmediateSuspensionDialog(): void {
  uiEventSubject.next({
    key: 'confirmation',
    data: {
      type: 'open',
      uiNamespace: `${uiNamespace}.suspend-dialog`,
      i18nScope: 'OrganizationalUnitSuspendDialog',
      title: tGlobal('OrganizationalUnitSuspendDialog.title'),
      content: tGlobal('OrganizationalUnitSuspendDialog.content'),
      onConfirm: async () => {
        await submitStatus(
          { start: dayjs().add(1, 'hour').toISOString() },
          'success.suspended'
        );
      },
    },
  });
}

/**
 * Opens the form dialog for scheduling a future suspension.
 */
function openScheduleSuspensionDialog(): void {
  uiEventSubject.next({
    key: 'form',
    data: {
      type: 'open',
      uiNamespace: `${uiNamespace}.schedule-suspension-dialog`,
      i18nScope: 'OrganizationalUnitScheduleSuspensionDialog',
      title: tGlobal('OrganizationalUnitScheduleSuspensionDialog.title'),
      content: tGlobal('OrganizationalUnitScheduleSuspensionDialog.content'),
      formFields:
        organizationalUnitLifecycleUiConfiguration['suspension.scheduled'],
      initialFormData: toOrganizationalUnitStatusForm(
        organizationalUnitStatus.value
      ),
      onSubmit: async (formData: OrganizationalUnitStatusForm) => {
        await submitStatus(formData, 'success.scheduled');
      },
    },
  });
}

/**
 * Opens the confirmation dialog for clearing the active suspension.
 *
 * The current backend rejects `suspensionPeriod` with a `start` in the past
 * and requires the field to be non-null, so a true reactivation endpoint is
 * not yet available. Pending backend support, the dialog surfaces a notice
 * to the user.
 */
function onClearSuspension(): void {
  uiEventSubject.next({
    key: 'confirmation',
    data: {
      type: 'open',
      uiNamespace: `${uiNamespace}.reactivate-dialog`,
      i18nScope: 'OrganizationalUnitReactivateDialog',
      title: tGlobal('OrganizationalUnitReactivateDialog.title'),
      content: tGlobal('OrganizationalUnitReactivateDialog.content'),
      onConfirm: async () => {
        Notify({
          type: 'warning',
          message: tGlobal('OrganizationalUnitReactivateDialog.unavailable'),
        });
      },
    },
  });
}

/**
 * Opens the form dialog for modifying the suspension end date while the OU
 * is currently suspended.
 */
function onModifySuspensionEnd(): void {
  const currentStart =
    organizationalUnitStatus.value?.suspensionPeriod?.start ?? null;
  uiEventSubject.next({
    key: 'form',
    data: {
      type: 'open',
      uiNamespace: `${uiNamespace}.edit-suspension-end-dialog`,
      i18nScope: 'OrganizationalUnitEditSuspensionEndDialog',
      title: tGlobal('OrganizationalUnitEditSuspensionEndDialog.title'),
      content: tGlobal('OrganizationalUnitEditSuspensionEndDialog.content'),
      formFields:
        organizationalUnitLifecycleUiConfiguration['suspension.modify'],
      initialFormData: toOrganizationalUnitStatusForm(
        organizationalUnitStatus.value
      ),
      onSubmit: async (formData: OrganizationalUnitStatusForm) => {
        if (currentStart == null) {
          return;
        }
        await submitStatus(
          { ...formData, start: currentStart },
          'success.endUpdated'
        );
      },
    },
  });
}

/**
 * Submits a status update to the backend and refreshes the local state.
 * The lifecycle form is converted into an API record (ISO dates) before the
 * call. Surfaces a positive notification on success and a negative one on
 * failure.
 * @param form - The lifecycle form values to convert and PUT to the backend.
 * @param successKey - The i18n key used for the success notification.
 */
async function submitStatus(
  form: OrganizationalUnitStatusForm,
  successKey: string
): Promise<void> {
  isLoading.value = true;
  try {
    const dto = await updateOrganizationalUnitStatus(
      organizationalUnitId.value,
      toOrganizationalUnitStatusRecord(form)
    );
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
  void loadOrganizationalUnit();
});
</script>
