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
  <q-layout view="hHh LpR fFf">
    <q-header
      v-bind="uiProps.header"
      data-cy="header"
    >
      <q-toolbar
        v-bind="uiProps.toolbar"
        data-cy="toolbar"
      >
        <q-avatar
          class="cursor-pointer"
          v-bind="uiProps.avatar"
          data-cy="application_logo"
          @click="goHome"
        >
          <q-img v-bind="uiProps.img"> </q-img>
        </q-avatar>
        <q-toolbar-title v-bind="uiProps.toolbarTitle">
          <div
            data-cy="application_title"
            class="cursor-pointer row inline"
            @click="goHome"
          >
            {{ t('title') }}
          </div>
        </q-toolbar-title>
        <q-badge
          v-bind="uiProps.badge"
          :label="t('version')"
          data-cy="application_version"
        />
        <q-space />

        <component
          :is="headerProfile"
          v-if="headerProfile"
          :ui-namespace="headerUiNamespace"
        />
      </q-toolbar>
      <q-toolbar
        v-bind="uiProps.toolbar"
        class="block"
        data-cy="navigation_toolbar"
      >
        <component
          :is="navigationMenu"
          v-if="navigationMenu"
          :items="uiStore.mainNavigationItems"
          :ui-namespace="headerUiNamespace"
        />
      </q-toolbar>
    </q-header>
    <q-page-container>
      <q-splitter v-bind="uiProps.splitter">
        <template #before>
          <organizational-units-tree />
        </template>

        <template #after>
          <router-view />
        </template>
      </q-splitter>
    </q-page-container>

    <!-- zone footer -->
  </q-layout>
  <!-- v8 ignore stop -->
</template>

<script setup lang="ts">
import type {
  LinidQAvatarProps,
  LinidQBadgeProps,
  LinidQHeaderProps,
  LinidQImgProps,
  LinidQSplitterProps,
  LinidQToolbarProps,
  LinidQToolbarTitleProps,
} from '@linagora/linid-im-front-corelib';
import {
  loadAsyncComponent,
  useLinidUiStore,
  useScopedI18n,
  useUiDesign,
} from '@linagora/linid-im-front-corelib';
import OrganizationalUnitsTree from 'components/tree/OrganizationalUnitsTree.vue';
import { useRouter } from 'vue-router';

const { ui } = useUiDesign();
const { t } = useScopedI18n('application');
const uiStore = useLinidUiStore();
const router = useRouter();

const navigationMenu = loadAsyncComponent('catalogUI/NavigationMenu');
const headerProfile = loadAsyncComponent('catalogUI/HeaderProfile');

const headerUiNamespace = 'splitter-layout.header';

const uiProps = {
  header: ui<LinidQHeaderProps>(headerUiNamespace, 'q-header'),
  toolbar: ui<LinidQToolbarProps>(headerUiNamespace, 'q-toolbar'),
  avatar: ui<LinidQAvatarProps>(headerUiNamespace, 'q-avatar'),
  toolbarTitle: ui<LinidQToolbarTitleProps>(
    headerUiNamespace,
    'q-toolbar-title'
  ),
  badge: ui<LinidQBadgeProps>(headerUiNamespace, 'q-badge'),
  img: ui<LinidQImgProps>(headerUiNamespace, 'q-img'),
  splitter: ui<LinidQSplitterProps>(headerUiNamespace, 'q-splitter'),
};

/**
 * Navigate to home page.
 */
function goHome() {
  router.push('/');
}
</script>
