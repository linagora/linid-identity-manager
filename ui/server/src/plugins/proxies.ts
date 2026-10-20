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

import {
  fastifyHttpProxy,
  type FastifyHttpProxyOptions,
} from '@fastify/http-proxy';
import type { FastifyInstance, FastifyRequest } from 'fastify';
import type { ServerConfig } from '../config/schema.js';

/** Methods that change data, refused when the request comes from another site (CSRF). */
const UNSAFE_METHODS = ['POST', 'PUT', 'PATCH', 'DELETE'];

/**
 * Removes a cookie from a `Cookie` header, keeping the other cookies.
 *
 * @param cookieHeader - Value of the `Cookie` header.
 * @param name - Name of the cookie to remove.
 * @returns The `Cookie` header without the cookie, or `undefined` if no cookie is left.
 */
function removeCookie(
  cookieHeader: string | undefined,
  name: string
): string | undefined {
  if (!cookieHeader) {
    return undefined;
  }

  // The browsers separate the cookies with "; "
  const otherCookies = cookieHeader
    .split('; ')
    .filter((cookie) => !cookie.startsWith(`${name}=`));

  if (otherCookies.length === 0) {
    return undefined;
  }
  return otherCookies.join('; ');
}

/**
 * Tells if a request comes from a page of this server: by `Sec-Fetch-Site` when the browser sends it, otherwise by
 * comparing `Origin` to the origin of the server. A request with neither header is refused.
 *
 * @param request - Incoming request.
 * @returns `true` if the request comes from this server.
 */
function isSameOrigin(request: FastifyRequest): boolean {
  const fetchSite = request.headers['sec-fetch-site'];
  if (fetchSite) {
    return fetchSite === 'same-origin';
  }
  return request.headers.origin === `${request.protocol}://${request.host}`;
}

/**
 * Registers the reverse proxies declared in `proxies`. The routes with `withAccessToken` send the access token of the
 * user session to the upstream, and answer 401 without calling it if the user is not logged in. They answer 403 to the
 * `POST`, `PUT`, `PATCH` and `DELETE` requests coming from another site.
 *
 * @param app - Fastify instance to configure.
 * @param config - Server configuration.
 * @param getFreshAccessToken - Gives the access token of the user session, or `null` if the user is not logged in.
 */
export function registerProxies(
  app: FastifyInstance,
  config: ServerConfig,
  getFreshAccessToken: (request: FastifyRequest) => Promise<string | null>
): void {
  config.proxies
    .map((route): FastifyHttpProxyOptions => ({
      prefix: route.prefix,
      upstream: route.upstream,
      rewritePrefix:
        route.rewritePrefix ?? (route.keepPrefix ? route.prefix : undefined),
      preHandler: route.withAccessToken
        ? async (request, reply) => {
            // Sec-Fetch-Site is set by the browser: same-origin means a page of this server
            if (
              UNSAFE_METHODS.includes(request.method) &&
              !isSameOrigin(request)
            ) {
              return reply
                .code(403)
                .send({ message: 'Cross-site request refused' });
            }

            const accessToken = await getFreshAccessToken(request);
            if (!accessToken) {
              return reply.code(401).send({ message: 'Authentication failed' });
            }
            request.headers.authorization = `Bearer ${accessToken}`;
            // The upstream is authenticated by the access token, it does not need the cookies of the browser
            delete request.headers.cookie;
          }
        : undefined,
      replyOptions: {
        rewriteRequestHeaders: (request, headers) => ({
          ...headers,
          // The session cookie is only meant for this server, the other cookies are kept (e.g. those of LemonLDAP::NG)
          cookie: removeCookie(headers.cookie, config.session.cookieName),
          host: request.host,
          'x-real-ip': request.ip,
          'x-forwarded-for': headers['x-forwarded-for']
            ? `${headers['x-forwarded-for']}, ${request.ip}`
            : request.ip,
          'x-forwarded-proto': request.protocol,
        }),
      },
    }))
    .forEach((options) => app.register(fastifyHttpProxy, options));
}
