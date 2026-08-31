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
      class="row q-gutter-x-sm organizational-unit-lifecycle-panel--actions"
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
  <!-- v8 ignore stop -->
</template>

<script setup lang="ts">
import type { DropdownClickPayload } from '@linagora/linid-im-front-corelib';
import {
  loadAsyncComponent,
  uiEventSubject,
  useNotify,
  useScopedI18n,
} from '@linagora/linid-im-front-corelib';
import axios from 'axios';
import { appConfig } from 'boot/config';
import { dayjs } from 'src/boot/dayjs';
import OrganizationalUnitSuspendedBanner from 'src/components/banner/OrganizationalUnitSuspendedBanner.vue';
import OrganizationalUnitSuspendedInfoText from 'src/components/text/OrganizationalUnitSuspendedInfoText.vue';
import { useLifecycleDialogs } from 'src/composables/useLifecycleDialogs';
import { useOrganizationalUnitLifecycleUi } from 'src/composables/useOrganizationalUnitLifecycleUi';
import { useOrganizationalUnitMapper } from 'src/composables/useOrganizationalUnitMapper';
import {
  reactivateOrganizationalUnit,
  suspendOrganizationalUnit,
} from 'src/services/OrganizationalUnitService';
import type { OrganizationalUnitLifecyclePanelProps } from 'src/types/organizationalUnitLifecyclePanelProps';
import type {
  OrganizationalUnitDTO,
  OrganizationalUnitStatus,
  OrganizationalUnitStatusForm,
} from 'src/types/organizationalUnits';
import { computed } from 'vue';

defineOptions({ inheritAttrs: false });

const props = defineProps<OrganizationalUnitLifecyclePanelProps>();

/** UI event key notifying the hosting details page that the OU status changed and the entity must be reloaded. */
const ORGANIZATIONAL_UNIT_STATUS_UPDATED_EVENT =
  'organizational-unit-status-updated';

const i18nScope = 'OrganizationalUnitLifecyclePanel';
const organizationalUnitLifecycleUiConfiguration =
  appConfig.organizationalUnitLifecycleFields;

const { t } = useScopedI18n(i18nScope);
const { Notify } = useNotify();
const {
  toOrganizationalUnitStatus,
  toOrganizationalUnitStatusForm,
  toOrganizationalUnitSuspensionRecord,
  toOrganizationalUnitReactivationRecord,
} = useOrganizationalUnitMapper();

const organizationalUnitId = computed(() => props.entity.id as string);

const organizationalUnitStatus = computed<OrganizationalUnitStatus | null>(
  () =>
    props.entity.id
      ? toOrganizationalUnitStatus(
          props.entity as unknown as OrganizationalUnitDTO
        )
      : null
);

const lifecycleUi = useOrganizationalUnitLifecycleUi(organizationalUnitStatus);

const dropdownButton = loadAsyncComponent('catalogUI/DropdownButton');

const { openFormDialog } = useLifecycleDialogs(props.uiNamespace);

const hasAnyLifecycleAction = computed(() =>
  Boolean(
    lifecycleUi.value?.activationMenuItems?.length ||
    lifecycleUi.value?.suspensionMenuItems?.length
  )
);

const actionDelay: number =
  appConfig?.immediateActionDelay > 0 ? appConfig.immediateActionDelay : 5;

/**
 * Dispatches a lifecycle action key (emitted by the dropdown button) to the matching dialog opening function.
 *
 * @param event - Click event payload emitted by the dropdown button.
 * @param event.key - Dotted lifecycle action key to dispatch, for example "suspension.immediate".
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
 * Opens the form dialog for an immediate suspension. Collects a reason, sub-reason and optional comment, then submits a
 * suspension period starting shortly from now with no end date.
 */
function openImmediateSuspensionDialog(): void {
  openFormDialog({
    i18nScope: 'OrganizationalUnitSuspendDialog',
    formFields:
      organizationalUnitLifecycleUiConfiguration['suspension.immediate'],
    onSubmit: (formData: OrganizationalUnitStatusForm) =>
      submitStatus(
        () =>
          suspendOrganizationalUnit(
            organizationalUnitId.value,
            toOrganizationalUnitSuspensionRecord({
              ...formData,
              start: dayjs().add(actionDelay, 'minute').toISOString(),
            })
          ),
        'success.suspended'
      ),
  });
}

/** Opens the form dialog for scheduling a future suspension. */
function openScheduleSuspensionDialog(): void {
  openFormDialog({
    i18nScope: 'OrganizationalUnitScheduleSuspensionDialog',
    formFields:
      organizationalUnitLifecycleUiConfiguration['suspension.scheduled'],
    onSubmit: (formData: OrganizationalUnitStatusForm) =>
      submitStatus(
        () =>
          suspendOrganizationalUnit(
            organizationalUnitId.value,
            toOrganizationalUnitSuspensionRecord(formData)
          ),
        'success.scheduled'
      ),
  });
}

/**
 * Opens the form dialog for reactivating a suspended organizational unit. The backend lifts the suspension by setting
 * its end to now.
 */
function onClearSuspension(): void {
  openFormDialog({
    i18nScope: 'OrganizationalUnitReactivateDialog',
    formFields:
      organizationalUnitLifecycleUiConfiguration['reactivation.immediate'],
    onSubmit: (formData: OrganizationalUnitStatusForm) =>
      submitStatus(
        () =>
          reactivateOrganizationalUnit(
            organizationalUnitId.value,
            toOrganizationalUnitReactivationRecord(formData)
          ),
        'success.reactivated'
      ),
  });
}

/**
 * Opens the form dialog for modifying the suspension end date while the OU is currently suspended, pre-filled with the
 * existing suspension period bounds.
 */
function onModifySuspensionEnd(): void {
  const currentStart =
    organizationalUnitStatus.value?.suspensionPeriod?.start ?? null;
  if (currentStart == null) {
    return;
  }

  openFormDialog({
    i18nScope: 'OrganizationalUnitEditSuspensionEndDialog',
    formFields:
      organizationalUnitLifecycleUiConfiguration['suspension.modify-end'],
    initialFormData: organizationalUnitStatus.value
      ? toOrganizationalUnitStatusForm(organizationalUnitStatus.value)
      : undefined,
    onSubmit: (formData: OrganizationalUnitStatusForm) =>
      submitStatus(
        () =>
          suspendOrganizationalUnit(
            organizationalUnitId.value,
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
 * Runs a status-update API call, then notifies the hosting details page through a UI event so it reloads the entity.
 * Surfaces a positive notification on success and a negative one on failure.
 *
 * @param statusUpdate - The status-mutation service call to execute, resolving to the updated OU DTO.
 * @param successKey - The i18n key used for the success notification.
 * @returns A promise that resolves once the OU status has been updated and the reload event has been emitted.
 */
async function submitStatus(
  statusUpdate: () => Promise<OrganizationalUnitDTO>,
  successKey: string
): Promise<void> {
  try {
    const dto = await statusUpdate();
    uiEventSubject.next({
      key: ORGANIZATIONAL_UNIT_STATUS_UPDATED_EVENT,
      data: dto,
    });
    Notify({ type: 'positive', message: t(successKey) });
  } catch (error) {
    const errorMessageKey =
      axios.isAxiosError(error) && error.response?.status === 400
        ? 'errors.validation'
        : 'errors.status';
    Notify({ type: 'negative', message: t(errorMessageKey) });
    throw error;
  }
}
</script>
