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
    class="row justify-center q-pa-md application-details-page"
    data-cy="application-details-page"
  >
    <div class="col-12 col-md-10 col-lg-10 application-details-page--content">
      <div
        class="row items-center justify-between q-mb-md application-details-page--header"
      >
        <div class="column application-details-page--heading">
          <h1
            class="q-ma-none text-h5 application-details-page--title"
            data-cy="application-details-page_title"
          >
            {{ t('title', application ?? {}) }}
          </h1>
          <p
            v-if="application?.description"
            class="q-ma-none text-subtitle1 text-grey-7 application-details-page--subtitle"
            data-cy="application-details-page_subtitle"
          >
            {{ t('subtitle', application) }}
          </p>
        </div>
        <div class="application-details-page--actions">
          <component
            :is="buttonsCard"
            v-if="buttonsCard"
            :ui-namespace="uiNamespace"
            :i18n-scope="i18nScope"
            :show-confirm-button="false"
            @cancel="goBack"
          />
        </div>
      </div>
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
import { getApplicationById } from 'src/services/ApplicationService';
import type { ApplicationDTO } from 'src/types/applications';
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const pageName = 'ApplicationDetailsPage';
const i18nScope = pageName;
const uiNamespace = 'applications.details-page';

const route = useRoute();
const router = useRouter();
const { t } = useScopedI18n(i18nScope);
const { Notify } = useNotify();

const applicationId = computed(() => route.params.id as string);

const application = ref<ApplicationDTO | null>(null);
const isLoading = ref<boolean>(false);

const buttonsCard = loadAsyncComponent('catalogUI/ButtonsCard');

/**
 * Loads the application data from the backend based on the route parameter. On failure, notifies the user and redirects
 * back to the applications list.
 */
async function loadApplication(): Promise<void> {
  isLoading.value = true;
  try {
    application.value = await getApplicationById(applicationId.value);
  } catch (error) {
    const errorMessageKey =
      axios.isAxiosError(error) && error.response?.status === 404
        ? 'errors.notFound'
        : 'errors.generic';
    Notify({
      type: 'negative',
      message: t(errorMessageKey),
    });
    goBack();
  } finally {
    isLoading.value = false;
  }
}

/** Navigates back to the applications list. */
function goBack(): void {
  router.push('/applications');
}

onMounted(() => {
  loadApplication();
});
</script>

<style scoped></style>
