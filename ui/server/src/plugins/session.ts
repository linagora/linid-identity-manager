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
import { fastifySession, type SessionStore } from '@fastify/session';
import type { FastifyInstance } from 'fastify';
import { createClient } from 'redis';
import type { ServerConfig } from '../config/schema.js';

declare module 'fastify' {
  /** Content of the session of a user. */
  interface Session {
    /** Login in progress, between `/auth/login` and `/auth/callback`. */
    login?: {
      /** Random value checked on the callback. */
      state: string;
      /** PKCE code verifier. */
      codeVerifier: string;
      /** Path the user is sent back to once logged in. */
      returnTo: string;
    };
    /** Tokens of the logged-in user. */
    tokens?: {
      /** JWT sent to the API. */
      accessToken: string;
      /** Token used to get a new access token once it has expired. */
      refreshToken?: string;
      /** ID token, sent back to the OpenID provider on logout. */
      idToken?: string;
      /** Expiration date of the access token, in milliseconds since the epoch. */
      expiresAt: number;
    };
    /** Claims of the ID token of the logged-in user. */
    user?: Record<string, unknown>;
  }
}

/**
 * Registers the session of the users: kept on the server side, in memory or in Redis, and identified by a cookie on the
 * browser side.
 *
 * The in-memory store only removes an expired session when its cookie comes back: the sessions that are never used
 * again stay in memory until the server restarts.
 *
 * @param app - Fastify instance to configure.
 * @param config - Server configuration.
 */
export async function registerSession(
  app: FastifyInstance,
  config: ServerConfig
): Promise<void> {
  const { store, redisUrl, cookie } = config.session;

  app.register(fastifyCookie);
  app.register(fastifySession, {
    secret: config.secrets.sessionCookieSecret,
    cookieName: cookie.name,
    saveUninitialized: false,
    rolling: true,
    // Without a store, the sessions are kept in memory
    store:
      store === 'redis'
        ? await createRedisStore(app, redisUrl, cookie.maxAgeSeconds)
        : undefined,
    cookie: {
      httpOnly: cookie.httpOnly,
      secure: cookie.secure,
      sameSite: cookie.sameSite,
      maxAge: cookie.maxAgeSeconds * 1000,
    },
  });
}

/**
 * Connects to Redis, and returns a session store keeping each session as JSON under the `session:<id>` key. The key
 * expires with the session: it is written again, with a new expiration, on every request of the user.
 *
 * @param app - Fastify instance, which closes the connection when it stops.
 * @param url - URL of Redis.
 * @param maxAgeSeconds - Lifetime of the sessions in seconds.
 * @returns The session store.
 */
async function createRedisStore(
  app: FastifyInstance,
  url: string | undefined,
  maxAgeSeconds: number
): Promise<SessionStore> {
  const client = createClient({ url });
  client.on('error', (error) => app.log.error({ err: error }, 'Redis error'));
  await client.connect();
  app.addHook('onClose', () => client.close());

  return {
    /**
     * Writes a session, which expires after `maxAgeSeconds`.
     *
     * @param sessionId - ID of the session.
     * @param session - Session to write.
     * @param callback - Called once written.
     */
    set(sessionId, session, callback) {
      client
        .set(`session:${sessionId}`, JSON.stringify(session), {
          expiration: { type: 'EX', value: maxAgeSeconds },
        })
        .then(() => callback(), callback);
    },
    /**
     * Reads a session.
     *
     * @param sessionId - ID of the session.
     * @param callback - Called with the session, or `null` when it does not exist or has expired.
     */
    get(sessionId, callback) {
      client
        .get(`session:${sessionId}`)
        .then(
          (json) => callback(null, json ? JSON.parse(json) : null),
          callback
        );
    },
    /**
     * Deletes a session.
     *
     * @param sessionId - ID of the session.
     * @param callback - Called once deleted.
     */
    destroy(sessionId, callback) {
      client.del(`session:${sessionId}`).then(() => callback(), callback);
    },
  };
}
