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

import type { FastifyInstance, FastifyRequest } from 'fastify';
import * as oidc from 'openid-client';
import type { ServerConfig } from '../config/schema.js';

/** Query string of `/auth/login`. */
interface LoginQuery {
  /** Path the user is sent back to once logged in. */
  returnTo?: string;
}

let oidcConfiguration: Promise<oidc.Configuration> | undefined;

/**
 * Fetches the OpenID provider metadata on first use, not at startup: the issuer can be served through this very server,
 * which is not listening yet at that time.
 *
 * @param config - Server configuration.
 * @returns The OpenID client configuration.
 */
function getOidcConfiguration(
  config: ServerConfig
): Promise<oidc.Configuration> {
  oidcConfiguration ??= oidc
    .discovery(
      new URL(config.oidc.issuer),
      config.oidc.clientId,
      config.secrets.oidcClientSecret
    )
    .catch((error) => {
      oidcConfiguration = undefined;
      throw error;
    });
  return oidcConfiguration;
}

/**
 * Registers the login and logout routes. The tokens of the user stay in their session, on the server side:
 *
 * - `GET /auth/login?returnTo=/path`: redirects to the OpenID provider.
 * - `GET /auth/callback`: gets the tokens, then redirects to `returnTo`.
 * - `GET /auth/me`: claims of the logged-in user, or 401.
 * - `POST` on `oidc.silentRenewPath` (e.g. `/auth/silent-renew`): renews the tokens with the refresh token, 200 or 401.
 * - `GET /auth/logout`: ends the session, then redirects to the logout page of the OpenID provider.
 * - `GET /auth/logged-out`: where the OpenID provider sends the user back after the logout, redirects to the SPA.
 *
 * Every other `/auth` path goes to the OpenID provider through the reverse proxy. These routes hide the ones of the
 * OpenID provider with the same path, such as the `/logout` of LemonLDAP::NG.
 *
 * @param app - Fastify instance to configure.
 * @param config - Server configuration.
 */
export function registerAuth(app: FastifyInstance, config: ServerConfig): void {
  app.get('/auth/login', async (request, reply) => {
    const { returnTo = '/' } = request.query as LoginQuery;
    const state = oidc.randomState();
    const codeVerifier = oidc.randomPKCECodeVerifier();
    request.session.login = {
      state,
      codeVerifier,
      // Only paths of this site, to not redirect to another one after the login
      returnTo: /^\/(?![/\\])/.test(returnTo) ? returnTo : '/',
    };

    const url = oidc.buildAuthorizationUrl(await getOidcConfiguration(config), {
      redirect_uri: config.oidc.redirectUri,
      scope: config.oidc.scope,
      state,
      code_challenge: await oidc.calculatePKCECodeChallenge(codeVerifier),
      code_challenge_method: 'S256',
    });
    return reply.redirect(url.href);
  });

  app.get('/auth/callback', async (request, reply) => {
    const { login } = request.session;
    if (!login) {
      return reply.redirect('/auth/login');
    }

    const tokens = await oidc.authorizationCodeGrant(
      await getOidcConfiguration(config),
      new URL(request.url, config.oidc.redirectUri),
      { expectedState: login.state, pkceCodeVerifier: login.codeVerifier }
    );

    // New session ID once logged in, against session fixation
    await request.session.regenerate();
    request.session.tokens = {
      accessToken: tokens.access_token,
      refreshToken: tokens.refresh_token,
      idToken: tokens.id_token,
      expiresAt: Date.now() + (tokens.expires_in ?? 0) * 1000,
    };
    request.session.user = tokens.claims();
    return reply.redirect(login.returnTo);
  });

  app.get('/auth/me', async (request, reply) => {
    if (!request.session.user) {
      return reply.code(401).send({ message: 'Not logged in' });
    }
    return request.session.user;
  });

  app.post(config.oidc.silentRenewPath, async (request, reply) => {
    if (request.headers['x-requested-with'] !== 'XMLHttpRequest') {
      return reply
        .code(403)
        .send({ message: 'Missing header X-Requested-With' });
    }
    const renewed = await renewTokens(request, config);
    return reply.code(renewed ? 200 : 401).send();
  });

  app.get('/auth/logout', async (request, reply) => {
    const idToken = request.session.tokens?.idToken;
    await request.session.destroy();

    const url = oidc.buildEndSessionUrl(await getOidcConfiguration(config), {
      post_logout_redirect_uri: config.oidc.postLogoutRedirectUri,
      ...(idToken && { id_token_hint: idToken }),
    });
    return reply.redirect(url.href);
  });

  app.get('/auth/logged-out', async (request, reply) => {
    await request.session.destroy();
    return reply.redirect('/');
  });
}

/**
 * Returns the access token of the logged-in user, renewed first when it expires in less than 30 seconds.
 *
 * @param request - Request of the user.
 * @param config - Server configuration.
 * @returns The access token, or `undefined` when nobody is logged in or the renewal fails.
 */
export async function getAccessToken(
  request: FastifyRequest,
  config: ServerConfig
): Promise<string | undefined> {
  const { tokens } = request.session;
  if (tokens && Date.now() > tokens.expiresAt - 30_000) {
    await renewTokens(request, config);
  }
  return request.session.tokens?.accessToken;
}

/**
 * Renews the tokens of the logged-in user with the refresh token. Ends the session when the renewal fails.
 *
 * @param request - Request of the user.
 * @param config - Server configuration.
 * @returns Whether the tokens have been renewed.
 */
async function renewTokens(
  request: FastifyRequest,
  config: ServerConfig
): Promise<boolean> {
  const { tokens } = request.session;
  if (!tokens?.refreshToken) {
    await request.session.destroy();
    return false;
  }

  try {
    const renewed = await oidc.refreshTokenGrant(
      await getOidcConfiguration(config),
      tokens.refreshToken
    );
    request.session.tokens = {
      accessToken: renewed.access_token,
      refreshToken: renewed.refresh_token ?? tokens.refreshToken,
      idToken: renewed.id_token ?? tokens.idToken,
      expiresAt: Date.now() + (renewed.expires_in ?? 0) * 1000,
    };
    return true;
  } catch (error) {
    request.log.warn({ err: error }, 'Token renewal failed');
    await request.session.destroy();
    return false;
  }
}
