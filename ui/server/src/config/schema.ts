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

/** Content of `server-config.json`, with `__APP_ENV__` replaced, and the secrets taken from environment variables. */
export interface ServerConfig {
  /** Environment name. */
  environment: string;
  /** Listening options. */
  http: {
    /** Listening port. */
    port: number;
    /** Certificate and private key paths. */
    tls?: {
      /** Path of the certificate. */
      cert: string;
      /** Path of the private key. */
      key: string;
    };
  };
  /** Base URLs of the services behind the reverse proxies. */
  upstreams: {
    /** REST API, behind `/backend`. */
    api: string;
    /** Module Federation remote, behind `/catalog-ui`. */
    catalogUi: string;
    /** LemonLDAP::NG, behind `/auth`, `/static` and `/index.psgi`. */
    auth: string;
  };
  /** OpenID Connect client used to log the users in. */
  oidc: {
    /** Public URL of the OpenID provider, also used by the server to reach it. */
    issuer: string;
    /** Client ID. */
    clientId: string;
    /** Requested scopes. */
    scope: string;
    /** Public URL of `/auth/callback`. */
    redirectUri: string;
    /** Public URL the OpenID provider sends the user back to after the logout: the one of `/auth/logged-out`. */
    postLogoutRedirectUri: string;
    /** Path of the route renewing the tokens, e.g. `/auth/silent-renew`. */
    silentRenewPath: string;
  };
  /** Session of the users, identified by a cookie. */
  session: {
    /** Where the sessions are kept: in the memory of the server, or in Redis. */
    store: 'memory' | 'redis';
    /** URL of Redis, e.g. `redis://redis:6379`, when `store` is `redis`. */
    redisUrl?: string;
    /** Session cookie. */
    cookie: {
      /** Name. */
      name: string;
      /** Whether the browser sends the cookie along with the requests coming from other sites. */
      sameSite: 'lax' | 'none' | 'strict';
      /** Sends the cookie over HTTPS only. */
      secure: boolean;
      /** Hides the cookie from the JavaScript of the page. */
      httpOnly: boolean;
      /** Lifetime of the session in seconds, extended on every request. */
      maxAgeSeconds: number;
    };
  };
  /** Directory of the built SPA. */
  staticDir: string;
  /** SPA configuration served at `/config.json`, with `__APP_ENV__` substituted. */
  configJsonPath: string;
  /** Cache headers of the static files. */
  cache: {
    /** `Cache-Control` of `index.html` and `config.json`. */
    html: string;
    /** Fingerprinted build output served under `/assets/`. */
    assets: {
      /** `max-age` in seconds. */
      maxAge: number;
      /** Adds `immutable` to the `Cache-Control` of the assets. */
      immutable: boolean;
    };
  };
  /** Secrets, not in `server-config.json`: read from environment variables. */
  secrets: {
    /** Secret of the OpenID Connect client, from `OIDC_CLIENT_SECRET`. */
    oidcClientSecret: string;
    /** Secret signing the session cookie, from `SESSION_COOKIE_SECRET`. At least 32 characters. */
    sessionCookieSecret: string;
  };
}
