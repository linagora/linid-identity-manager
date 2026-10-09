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

import { readFile } from 'node:fs/promises';
import { relative, resolve, sep } from 'node:path';
import { fastifyStatic } from '@fastify/static';
import type { FastifyInstance } from 'fastify';
import type { ServerConfig } from '../config/schema.js';

/** URL prefix of the assets. */
const ASSETS_PREFIX = '/assets/';

/**
 * Serves the SPA: static files from `staticDir`, `config.json` with `__APP_ENV__` substituted, and `index.html` for any
 * other `GET` so that the client-side router handles it.
 *
 * @param app - Fastify instance to configure.
 * @param config - Server configuration.
 */
export function registerStaticFiles(
  app: FastifyInstance,
  config: ServerConfig
): void {
  const { html, assets } = config.cache;

  const root = resolve(config.staticDir);

  app.register(fastifyStatic, {
    root,
    cacheControl: false,
    setHeaders: (reply, filePath) => {
      const path = relative(root, filePath).split(sep).join('/');
      if (path === 'index.html') {
        reply.header('Cache-Control', html);
      } else if (`/${path}`.startsWith(ASSETS_PREFIX)) {
        reply.header('Cache-Control', assets);
      }
    },
  });

  // Read on every request so that changes to the mounted file apply without restarting the server
  app.get('/config.json', async (_request, reply) => {
    const configJson = await readFile(config.configJsonPath, 'utf-8');
    return reply
      .header('Cache-Control', html)
      .type('application/json')
      .send(configJson.replaceAll('__APP_ENV__', config.environment));
  });

  app.setNotFoundHandler((request, reply) => {
    const isPageRequest =
      (request.method === 'GET' || request.method === 'HEAD') &&
      !request.url.startsWith(ASSETS_PREFIX);
    if (isPageRequest) {
      return reply.sendFile('index.html');
    }
    return reply.code(404).send({
      error: `Unknown route: ${request.url}`,
      errorKey: 'error.router.unknown.route',
      errorContext: { route: request.url },
      status: 404,
      timestamp: Date.now(),
    });
  });
}
