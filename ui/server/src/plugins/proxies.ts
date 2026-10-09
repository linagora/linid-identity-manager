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

import { fastifyHttpProxy } from '@fastify/http-proxy';
import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import type { ServerConfig } from '../config/schema.js';
import { getAccessToken } from './auth.js';

/** Reverse proxy route. */
interface ProxyRoute {
  /** Public URL prefix. */
  prefix: string;
  /** Base URL of the proxied service. */
  upstream: string;
  /** Keeps the prefix in the upstream path. */
  keepPrefix?: boolean;
  /** Runs before the request is forwarded. */
  preHandler?: (
    request: FastifyRequest,
    reply: FastifyReply
  ) => Promise<unknown>;
}

/**
 * Registers the reverse proxies to the API, the Module Federation remote and LemonLDAP::NG.
 *
 * The requests to the API must have the `X-Requested-With: XMLHttpRequest` header, which another site cannot send along
 * with the session cookie (CSRF). They get the access token of the logged-in user, if any.
 *
 * @param app - Fastify instance to configure.
 * @param config - Server configuration.
 */
export function registerProxies(
  app: FastifyInstance,
  config: ServerConfig
): void {
  const { upstreams } = config;

  /**
   * Rejects the requests without the CSRF header, and adds the access token of the logged-in user.
   *
   * @param request - Request to the API.
   * @param reply - Reply, sent only when the CSRF header is missing.
   * @returns The 403 reply when the CSRF header is missing.
   */
  async function addAccessToken(request: FastifyRequest, reply: FastifyReply) {
    if (request.headers['x-requested-with'] !== 'XMLHttpRequest') {
      return reply
        .code(403)
        .send({ message: 'Missing header X-Requested-With' });
    }
    const accessToken = await getAccessToken(request, config);
    if (accessToken) {
      request.headers.authorization = `Bearer ${accessToken}`;
    }
  }

  const routes: ProxyRoute[] = [
    {
      prefix: '/backend',
      upstream: upstreams.api,
      preHandler: addAccessToken,
    },
    { prefix: '/catalog-ui', upstream: upstreams.catalogUi },
    { prefix: '/auth', upstream: upstreams.auth },
    { prefix: '/static', upstream: upstreams.auth, keepPrefix: true },
    { prefix: '/index.psgi', upstream: upstreams.auth, keepPrefix: true },
  ];
  for (const route of routes) {
    app.register(fastifyHttpProxy, {
      prefix: route.prefix,
      upstream: route.upstream,
      rewritePrefix: route.keepPrefix ? route.prefix : undefined,
      preHandler: route.preHandler,
      replyOptions: {
        rewriteRequestHeaders: (request, headers) => ({
          ...headers,
          host: request.host,
          'x-real-ip': request.ip,
          'x-forwarded-for': headers['x-forwarded-for']
            ? `${headers['x-forwarded-for']}, ${request.ip}`
            : request.ip,
          'x-forwarded-proto': request.protocol,
        }),
      },
    });
  }
}
