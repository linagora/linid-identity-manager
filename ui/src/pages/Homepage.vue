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
    data-cy="home-page"
    class="q-pa-md flex justify-center"
  >
    <q-card class="q-pa-md q-mb-lg shadow-2 self-start home-page--info">
      <!-- eslint-disable vue/no-v-text-v-html-on-component vue/no-v-html -->
      <q-card-section
        class="q-pa-sm"
        data-cy="home-page-intro"
        v-html="t('intro')"
      />
      <q-card-section
        class="q-pa-sm"
        data-cy="home-page-opensource"
        v-html="t('opensource')"
      />
      <q-card-section
        class="q-pa-sm"
        data-cy="home-page-license"
        v-html="t('license')"
      />
      <q-card-section
        class="q-pa-sm"
        data-cy="home-page-links"
        v-html="t('links')"
      />
      <q-separator />
      <q-card-section
        class="q-pa-sm"
        data-cy="home-page-branding"
        v-html="t('branding')"
      />
      <!-- eslint-enable vue/no-v-text-v-html-on-component vue/no-v-html -->
    </q-card>

    <component
      :is="treeComponent"
      v-if="treeComponent"
      v-model:selected-node="selectedNode"
      ui-namespace="tree"
      i18n-scope="Homepage"
      :nodes="treeNodes"
      :node-types="treeNodeTypes"
      @click:edit="handleEdit"
      @click:delete="handleDelete"
      @click:view="handleView"
      @update:selected-node="handleSelected"
    ></component>
    <span>TOOTOTO</span>
  </q-page>
</template>

<script setup lang="ts">
import {
  loadAsyncComponent,
  useScopedI18n,
} from '@linagora/linid-im-front-corelib';
import type { TreeNodeType, TreeNode } from '@linagora/linid-im-front-corelib';
import { ref } from 'vue';

const treeComponent = loadAsyncComponent('catalogUI/GenericTree');

const { t } = useScopedI18n('Homepage');

/**
 * A.
 * @param props A.
 */
function handleEdit(props: TreeNode<string>) {
  console.log('Edit = ', props);
}

/**
 * A.
 * @param props A.
 */
function handleDelete(props: TreeNode<string>) {
  console.log('Delete = ', props);
}

/**
 * A.
 * @param props A.
 */
function handleView(props: TreeNode<string>) {
  console.log('View = ', props);
}

/**
 * A.
 * @param key A.
 */
function handleSelected(key: string) {
  console.log('Selected HANDLEEE = ', key);
}

const selectedNode = ref<string>('org-1');

const treeNodeTypes: TreeNodeType[] = [
  {
    type: 'Structure',
    actions: ['edit'],
  },
  {
    type: 'Establishment',
    actions: ['edit'],
  },
];

const treeNodes: TreeNode<string>[] = [
  {
    type: 'Structure',
    key: 'org-1',
    value: 'Linagora avec une phrasess',
    extraActions: ['delete', 'view'],
    nodes: [
      {
        type: 'Establishment',
        key: 'est-1',
        value: 'Establishement 111',
        extraActions: ['delete'],
        nodes: [],
      },
      {
        type: 'Establishment',
        key: 'est-2',
        value: 'Establishment 2',
        extraActions: ['delete'],
        nodes: [],
      },
    ],
  },
];
</script>

<style scoped>
.home-page--info {
  width: 100%;
  max-width: 700px;
}
</style>
