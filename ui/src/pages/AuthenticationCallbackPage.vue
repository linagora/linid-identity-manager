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
  <q-page class="row items-center justify-evenly">
    <div>
      {{ t('processing') }}
    </div>
  </q-page>
</template>

<script lang="ts" setup>
import { useScopedI18n } from '@linagora/linid-im-front-corelib';
import { oidcClient } from 'src/boot/oidc';
import { onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const router = useRouter();
const route = useRoute();
const { t } = useScopedI18n('AuthenticationCallbackPage');

/**
 * Represents the optional redirect state used during OIDC authentication. This is typically used to preserve the
 * original destination URL so the user can be redirected back after successful login.
 */
interface OidcRedirectState {
  /** The URL to redirect to after authentication. If not provided, the default route is used. */
  redirectUrl?: string;
}

onMounted(async () => {
  try {
    if (route.path.endsWith('/callback/logout')) {
      await oidcClient.signoutRedirectCallback();
      await oidcClient.removeUser();
      await router.replace('/');
    } else {
      const user = await oidcClient.signinRedirectCallback();
      const state = user.state as OidcRedirectState;
      let redirectUrl = state?.redirectUrl || '/';
      if (!redirectUrl.startsWith('/') || redirectUrl.startsWith('//')) {
        redirectUrl = '/';
      }
      await router.replace(redirectUrl);
    }
  } catch (error) {
    console.error('Error during OIDC callback processing:', error);
    await router.replace('/');
  }
});
</script>
