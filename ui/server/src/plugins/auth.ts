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
import * as client from 'openid-client';
import type { ServerConfig } from '../config/schema.js';

/** Request of `GET /auth/login`. */
interface LoginRequest {
  /** Query string. */
  Querystring: {
    /** Path of the SPA to go back to after the login. */
    returnTo?: string;
  };
}

/**
 * Registers the OpenID Connect authentication routes under `/auth`.
 *
 * @param app - Fastify instance to configure.
 * @param config - Server configuration.
 * @returns A function giving the access token of the user session, refreshed if it expires soon.
 */
export function registerAuth(
  app: FastifyInstance,
  config: ServerConfig
): (request: FastifyRequest) => Promise<string | null> {
  const { oidc } = config;
  const redirectUri = `${oidc.publicUrl}/auth/callback`;

  let oidcConfig: client.Configuration | undefined;

  /**
   * Returns the configuration of the issuer, discovered on first use and then kept in memory. Not discovered at
   * startup: in e2e, the issuer is reached through this server, which is not listening yet.
   *
   * @returns The configuration of the issuer.
   */
  async function getOidcConfig(): Promise<client.Configuration> {
    oidcConfig ??= await client.discovery(
      new URL(oidc.issuer),
      oidc.clientId,
      oidc.clientSecret
    );
    return oidcConfig;
  }

  /**
   * Returns the access token of the user session. If it expires in less than 30 seconds, it is first renewed with the
   * refresh token. If it cannot be renewed (no refresh token, refresh token expired or revoked), the session is
   * destroyed.
   *
   * @param request - Request of the user.
   * @returns The access token, or `null` if the user is not logged in.
   */
  async function getFreshAccessToken(
    request: FastifyRequest
  ): Promise<string | null> {
    const { tokens } = request.session;
    if (!tokens) {
      return null;
    }
    if (tokens.expiresAt - 30_000 > Date.now()) {
      return tokens.accessToken;
    }

    if (tokens.refreshToken) {
      try {
        const refreshed = await client.refreshTokenGrant(
          await getOidcConfig(),
          tokens.refreshToken
        );
        request.session.tokens = {
          accessToken: refreshed.access_token,
          // The issuer may not send a new refresh token or ID token: the current ones are kept
          refreshToken: refreshed.refresh_token ?? tokens.refreshToken,
          idToken: refreshed.id_token ?? tokens.idToken,
          expiresAt: Date.now() + (refreshed.expiresIn() ?? 0) * 1000,
        };
        return refreshed.access_token;
      } catch (error) {
        request.log.warn({ err: error }, 'Token refresh failed');
      }
    }

    await request.session.destroy();
    return null;
  }

  app.get<LoginRequest>('/auth/login', async (request, reply) => {
    const state = client.randomState();
    const nonce = client.randomNonce();
    const codeVerifier = client.randomPKCECodeVerifier();
    request.session.login = {
      state,
      nonce,
      codeVerifier,
      returnTo: request.query.returnTo ?? '/',
    };

    const authorizationUrl = client.buildAuthorizationUrl(
      await getOidcConfig(),
      {
        redirect_uri: redirectUri,
        scope: oidc.scope,
        state,
        nonce,
        code_challenge: await client.calculatePKCECodeChallenge(codeVerifier),
        code_challenge_method: 'S256',
      }
    );
    return reply.redirect(authorizationUrl.href);
  });

  app.get('/auth/callback', async (request, reply) => {
    const { login } = request.session;
    if (!login) {
      return reply.code(400).send({ message: 'No login in progress' });
    }

    // No redirection on error, to avoid a login loop: rejected state, expired code, unreachable issuer...
    try {
      const tokens = await client.authorizationCodeGrant(
        await getOidcConfig(),
        new URL(request.url, oidc.publicUrl),
        {
          pkceCodeVerifier: login.codeVerifier,
          expectedState: login.state,
          expectedNonce: login.nonce,
        }
      );

      // New session ID once logged in (session fixation); the old session, with the login data, is destroyed
      await request.session.regenerate();
      request.session.tokens = {
        accessToken: tokens.access_token,
        refreshToken: tokens.refresh_token,
        // Always present: openid-client requires it because a nonce is expected
        idToken: tokens.id_token as string,
        // Without `expires_in`, the access token is considered expired and is refreshed on first use
        expiresAt: Date.now() + (tokens.expiresIn() ?? 0) * 1000,
      };
      request.session.claims = tokens.claims();

      return reply.redirect(login.returnTo);
    } catch (error) {
      request.log.warn({ err: error }, 'Login callback failed');
      return reply.code(400).send({ message: 'Login failed' });
    }
  });

  app.get('/auth/me', async (request, reply) => {
    if (!request.session.tokens) {
      return reply.code(401).send({ message: 'Not logged in' });
    }
    return request.session.claims;
  });

  app.get('/auth/logout', async (request, reply) => {
    const parameters: Record<string, string> = {
      post_logout_redirect_uri: `${oidc.publicUrl}/auth/logged-out`,
    };
    const idToken = request.session.tokens?.idToken;
    if (idToken) {
      parameters.id_token_hint = idToken;
    }
    const logoutUrl = client.buildEndSessionUrl(
      await getOidcConfig(),
      parameters
    );

    // Removes the session from the store; the cookie is not cleared by @fastify/session
    await request.session.destroy();
    reply.clearCookie(config.session.cookieName, { path: '/' });
    return reply.redirect(logoutUrl.href);
  });

  app.get('/auth/logged-out', async (_request, reply) =>
    reply.redirect('/auth/login')
  );

  return getFreshAccessToken;
}
