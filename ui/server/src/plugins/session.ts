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

import { fastifyCookie } from '@fastify/cookie';
import { fastifySession } from '@fastify/session';
import type { FastifyInstance } from 'fastify';
import type { ServerConfig } from '../config/schema.js';

declare module 'fastify' {
  /** Data stored in the user session. */
  interface Session {
    /** Login in progress, from `/auth/login` to `/auth/callback`. */
    login?: {
      /** Expected `state` parameter of the callback. */
      state: string;
      /** Expected `nonce` claim of the ID token. */
      nonce: string;
      /** PKCE code verifier. */
      codeVerifier: string;
      /** Path of the SPA to go back to after the login. */
      returnTo: string;
    };
    /** Tokens of the logged-in user, never sent to the browser. */
    tokens?: {
      /** Access token sent to the API. */
      accessToken: string;
      /** Refresh token, used to renew the access token. */
      refreshToken?: string;
      /** ID token, used as a hint for the logout. */
      idToken: string;
      /** Expiration date of the access token, in milliseconds since the epoch. */
      expiresAt: number;
    };
    /** Claims of the logged-in user, returned by `/auth/me`. */
    claims?: Record<string, unknown>;
  }
}

/**
 * Registers the user session, stored in server memory (default store of `@fastify/session`) and identified by a cookie.
 *
 * @param app - Fastify instance to configure.
 * @param config - Server configuration.
 */
export function registerSession(
  app: FastifyInstance,
  config: ServerConfig
): void {
  app.register(fastifyCookie);
  app.register(fastifySession, {
    secret: config.session.secret,
    cookieName: config.session.cookieName,
    saveUninitialized: false,
    cookie: {
      httpOnly: true,
      sameSite: config.session.sameSite ?? 'lax',
      secure: config.session.secure ?? true,
      maxAge: config.session.maxAgeSeconds * 1000,
      path: '/',
    },
  });
}
