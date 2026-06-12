/*
 * Copyright (C) 2025 Linagora
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

import { defineRouter } from '#q-app/wrappers';
import { useLinidUserStore } from '@linagora/linid-im-front-corelib';
import { getUser, oidcClient } from 'src/boot/oidc';
import {
  createMemoryHistory,
  createRouter,
  createWebHashHistory,
  createWebHistory,
} from 'vue-router';
import routes from './routes';

/*
 * If not building with SSR mode, you can
 * directly export the Router instantiation;
 *
 * The function below can be async too; either use
 * async/await or return a Promise which resolves
 * with the Router instance.
 */
let userReady = false;
let unloadListenerRegistered = false;

/**
 * Creates and configures the Vue Router instance.
 *
 * @returns The Vue Router instance.
 */
export default defineRouter(function (/* { store, ssrContext } */) {
  const createHistory = process.env.SERVER
    ? createMemoryHistory
    : process.env.VUE_ROUTER_MODE === 'history'
      ? createWebHistory
      : createWebHashHistory;

  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,

    // Leave this as is and make changes in quasar.conf.js instead!
    // quasar.conf.js -> build -> vueRouterMode
    // quasar.conf.js -> build -> publicPath
    history: createHistory(process.env.VUE_ROUTER_BASE),
  });

  Router.beforeEach(async (to, from, next) => {
    // Registered lazily: the OIDC boot finishes asynchronously after the
    // router factory, so `oidcClient` may still be undefined at factory
    // time. The first navigation always happens after all boots resolved.
    if (!unloadListenerRegistered && oidcClient) {
      oidcClient.events.addUserUnloaded(() => {
        userReady = false;
      });
      unloadListenerRegistered = true;
    }

    const requiresAuth = to.matched.some((record) => record.meta.requiresAuth);
    if (!requiresAuth) {
      return next();
    }
    const user = await getUser();

    if (user) {
      if (!userReady) {
        const userStore = useLinidUserStore();
        userStore.setUserFromClaims(user.profile);
        userReady = true;
      }

      next();
    } else {
      try {
        await oidcClient.signinRedirect({
          state: { redirectUrl: to.fullPath },
        });
      } catch (error) {
        console.error('Failed to initiate OIDC sign-in redirect:', error);
      }
      next(false);
    }
  });

  return Router;
});
