/*
 * Copyright (C) 2026 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
 * which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
 * of the interface window, the display of the “You are using the Open Source and free version of LinID™, powered by
 * Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!” infobox and in the e-mails
 * sent with the Program, notice appended to any type of outbound messages (e.g. e-mail and meeting requests) as well
 * as in the LinID Identity Manager user interface, (ii) retain all hypertext links between LinID Identity Manager
 * and https://linid.org/, as well as between LINAGORA and LINAGORA.com, and (iii) refrain from infringing LINAGORA
 * intellectual property rights over its trademarks and commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License and its applicable Additional Terms for
 * LinID Identity Manager along with this program. If not, see <http://www.gnu.org/licenses/> for the GNU Affero
 * General Public License version 3 and <http://www.linagora.com/licenses/> for the Additional Terms applicable to the
 * LinID Identity Manager software.
 */

import { setHttpClient } from '@linagora/linid-im-front-corelib';
import type { AxiosError } from 'axios';
import axios, { type AxiosInstance } from 'axios';
import { getUser, oidcClient } from './oidc';
import { defineBoot } from '#q-app/wrappers';

declare module 'vue' {
  /** Augmenting Vue's ComponentCustomProperties interface to include $axios and $api properties for Axios instances. */
  interface ComponentCustomProperties {
    // eslint-disable-next-line jsdoc/require-jsdoc
    $axios: AxiosInstance;
    // eslint-disable-next-line jsdoc/require-jsdoc
    $api: AxiosInstance;
  }
}

// Be careful when using SSR for cross-request state pollution
// due to creating a Singleton instance here;
// If any client changes this (global) instance, it might be a
// good idea to move this instance creation inside of the
// "export default () => {}" function below (which runs individually
// for each client)
const api = axios.create({ baseURL: '/backend', timeout: 30000 });
let redirecting = false;

api.interceptors.request.use(async (config) => {
  try {
    const user = await getUser();

    if (user && user.access_token) {
      config.headers.Authorization = `Bearer ${user.access_token}`;
    }
  } catch (error) {
    console.warn('Error retrieving OIDC token', error);
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    if (error.response?.status === 401 && !redirecting) {
      redirecting = true;
      console.warn(
        'Received 401 on',
        error.config?.url,
        '— redirecting to sign-in'
      );
      try {
        await oidcClient.removeUser();
        await oidcClient.signinRedirect();
      } catch (redirectError) {
        redirecting = false;
        console.warn('OIDC redirect failed', redirectError);
      }
    }

    return Promise.reject(error);
  }
);

/**
 * Boot file to configure Axios and add it to the global Vue properties.
 *
 * @param app - The Vue application instance.
 */
export default defineBoot(({ app }) => {
  // Initialize the shared HTTP client for corelib and remote modules
  setHttpClient(api);

  // for use inside Vue files (Options API) through this.$axios and this.$api

  app.config.globalProperties.$axios = axios;
  // ^ ^ ^ this will allow you to use this.$axios (for Vue Options API form)
  //       so you won't necessarily have to import axios in each vue file

  app.config.globalProperties.$api = api;
  // ^ ^ ^ this will allow you to use this.$api (for Vue Options API form)
  //       so you can easily perform requests against your app's API
});

export { api };
