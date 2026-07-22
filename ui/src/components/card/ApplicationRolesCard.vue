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
    class="application-roles-card"
    data-cy="application-roles-card"
  >
    <div
      class="row items-center justify-between q-mb-md application-roles-card--header"
    >
      <h2
        class="q-ma-none text-h6 application-roles-card--title"
        data-cy="application-roles-card_title"
      >
        {{ t('roles.title') }}
      </h2>
      <q-btn
        v-bind="uiProps.addRoleButton"
        :label="t('roles.addButton')"
        data-cy="application-roles-card_add-role-button"
        @click="openCreateRoleDialog"
      />
    </div>
    <component
      :is="rolesTable"
      v-if="rolesTable"
      :ui-namespace="`${uiNamespace}.roles`"
      :i18n-scope="i18nScope"
      :rows="roleRows"
      :columns="roleColumns"
      :loading="isLoading || isSavingRoles"
      row-key="name"
    >
      <template #actions="{ row }">
        <q-btn
          v-bind="uiProps.editRoleButton"
          :label="t('roles.editButton')"
          :data-cy="`role-edit-button_${row.name}`"
          @click="openEditRoleDialog(String(row.name))"
        />
        <q-btn
          v-bind="uiProps.deleteRoleButton"
          :label="t('roles.deleteButton')"
          :data-cy="`role-delete-button_${row.name}`"
          @click="openDeleteRoleDialog(String(row.name))"
        />
      </template>
    </component>
  </div>
  <!-- v8 ignore stop -->
</template>

<script setup lang="ts">
import {
  type LinidQBtnProps,
  loadAsyncComponent,
  useNotify,
  useScopedI18n,
  useUiDesign,
} from '@linagora/linid-im-front-corelib';
import axios from 'axios';
import { appConfig } from 'boot/config';
import type { QTableColumn } from 'quasar';
import { useLifecycleDialogs } from 'src/composables/useLifecycleDialogs';
import {
  getApplicationRoles,
  updateApplicationRoles,
} from 'src/services/ApplicationService';
import type { ApplicationRolesCardProps } from 'src/types/applicationRolesCardProps';
import type { ApplicationRole } from 'src/types/applications';
import { computed, onMounted, ref } from 'vue';

const props = defineProps<ApplicationRolesCardProps>();

const i18nScope = 'ApplicationDetailsPage';
const uiNamespace = 'applications.details-page';
const roleFormFields = appConfig.applicationRoleFields;

const { t } = useScopedI18n(i18nScope);
const { Notify } = useNotify();
const { ui } = useUiDesign();

const roles = ref<ApplicationRole[]>([]);
const isLoading = ref<boolean>(false);
const isSavingRoles = ref<boolean>(false);

const rolesTable = loadAsyncComponent('catalogUI/GenericEntityTable');

const { openFormDialog, openConfirmationDialog } =
  useLifecycleDialogs(uiNamespace);

const uiProps = computed(() => ({
  addRoleButton: ui<LinidQBtnProps>(`${uiNamespace}.roles.add-button`, 'q-btn'),
  editRoleButton: ui<LinidQBtnProps>(
    `${uiNamespace}.roles.edit-button`,
    'q-btn'
  ),
  deleteRoleButton: ui<LinidQBtnProps>(
    `${uiNamespace}.roles.delete-button`,
    'q-btn'
  ),
}));

const roleColumns = computed<QTableColumn[]>(() => [
  {
    name: 'name',
    label: t('roles.columns.name'),
    field: 'name',
    align: 'left',
    sortable: true,
  },
  {
    name: 'description',
    label: t('roles.columns.description'),
    field: 'description',
    align: 'left',
  },
  {
    name: 'table_actions',
    label: t('roles.columns.actions'),
    field: 'name',
    align: 'right',
  },
]);

const roleRows = computed<Record<string, unknown>[]>(() =>
  roles.value.map((role) => ({
    name: role.name,
    description: role.description ?? '',
  }))
);

/** Loads the roles of the application from the backend. On failure, notifies the user. */
async function loadRoles(): Promise<void> {
  isLoading.value = true;
  try {
    roles.value = await getApplicationRoles(props.applicationId);
  } catch {
    Notify({
      type: 'negative',
      message: t('roles.errors.load'),
    });
  } finally {
    isLoading.value = false;
  }
}

/**
 * Normalizes a role form payload into an application role: the name is trimmed and an empty description is dropped.
 *
 * @param formData - The raw role form payload submitted by the dialog.
 * @returns The normalized application role.
 */
function toRole(formData: ApplicationRole): ApplicationRole {
  const description = formData.description?.trim();
  return {
    name: formData.name.trim(),
    ...(description ? { description } : {}),
  };
}

/**
 * Ensures no other role already uses the given name. Notifies the user and throws when the name is already taken, so
 * the calling dialog stays open for correction.
 *
 * @param name - The role name to check.
 * @param previousName - The original name of the edited role, when editing; undefined when creating.
 */
function assertRoleNameAvailable(name: string, previousName?: string): void {
  const isTaken = roles.value.some(
    (role) => role.name === name && role.name !== previousName
  );

  if (isTaken) {
    Notify({
      type: 'negative',
      message: t('roles.errors.duplicateName', { name }),
    });
    throw new Error(`Role name already used: ${name}`);
  }
}

/**
 * Persists the given full list of roles through `PUT /applications/{id}/roles` and replaces the local roles state with
 * the payload returned by the backend, which is the single source of truth (no re-fetch).
 *
 * @param newRoles - The new full list of roles to persist.
 * @param successMsgKey - The i18n key of the success notification, relative to the component scope.
 */
async function saveRoles(
  newRoles: ApplicationRole[],
  successMsgKey: string
): Promise<void> {
  isSavingRoles.value = true;
  try {
    roles.value = await updateApplicationRoles(props.applicationId, newRoles);
    Notify({
      type: 'positive',
      message: t(successMsgKey),
    });
  } catch (error) {
    const errorMsg = axios.isAxiosError(error)
      ? (error.response?.data?.error ?? t('roles.errors.update'))
      : t('roles.errors.update');

    Notify({
      type: 'negative',
      message: errorMsg,
    });
    throw error;
  } finally {
    isSavingRoles.value = false;
  }
}

/** Opens the role creation form dialog and appends the new role to the list on submit. */
function openCreateRoleDialog(): void {
  openFormDialog<ApplicationRole>({
    i18nScope: 'ApplicationRoleActions.FormDialog.create',
    formFields: roleFormFields,
    onSubmit: (formData) => {
      const role = toRole(formData);
      assertRoleNameAvailable(role.name);
      return saveRoles([...roles.value, role], 'roles.createSuccess');
    },
  });
}

/**
 * Opens the role edition form dialog pre-filled with the given role and replaces it in the list on submit.
 *
 * @param roleName - The name of the role to edit.
 */
function openEditRoleDialog(roleName: string): void {
  const role = roles.value.find((item) => item.name === roleName);

  if (!role) {
    return;
  }

  openFormDialog<ApplicationRole>({
    i18nScope: 'ApplicationRoleActions.FormDialog.edit',
    formFields: roleFormFields,
    initialFormData: { ...role },
    onSubmit: (formData) => {
      const updatedRole = toRole(formData);
      assertRoleNameAvailable(updatedRole.name, roleName);
      const newRoles = roles.value.map((item) =>
        item.name === roleName ? updatedRole : item
      );
      return saveRoles(newRoles, 'roles.editSuccess');
    },
  });
}

/**
 * Opens the role deletion confirmation dialog and removes the role from the list on confirmation.
 *
 * @param roleName - The name of the role to delete.
 */
function openDeleteRoleDialog(roleName: string): void {
  openConfirmationDialog({
    i18nScope: 'ApplicationRoleActions.ConfirmationDialog.delete',
    i18nParams: { name: roleName },
    onConfirm: () =>
      saveRoles(
        roles.value.filter((role) => role.name !== roleName),
        'roles.deleteSuccess'
      ),
  });
}

onMounted(() => {
  loadRoles();
});
</script>

<style scoped></style>
