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

/** Reverse proxy route. */
export interface ProxyRoute {
  /** Public URL prefix. */
  prefix: string;
  /** Base URL of the proxied service. */
  upstream: string;
  /** Keeps the prefix in the upstream path. */
  keepPrefix?: boolean;
  /** Sends the access token of the user session in the `Authorization` header. */
  withAccessToken?: boolean;
}

/** Content of `server-config.json`, with `__APP_ENV__` replaced. */
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
  /** OpenID Connect client used to authenticate users. */
  oidc: {
    /** URL of the OpenID Connect issuer. */
    issuer: string;
    /** Client identifier. */
    clientId: string;
    /** Client secret, absent for a public client. */
    clientSecret?: string;
    /** Requested scopes, separated by spaces. */
    scope: string;
    /** Public URL of the application, used to build the redirect URIs. */
    publicUrl: string;
  };
  /** User session, stored in server memory and identified by a cookie. */
  session: {
    /** Secret used to sign the session cookie, at least 32 characters. */
    secret: string;
    /** Name of the session cookie. */
    cookieName: string;
    /** Sends the session cookie over HTTPS only, `true` by default. */
    secure?: boolean;
    /** `SameSite` attribute of the session cookie, `lax` by default. */
    sameSite?: 'lax' | 'strict' | 'none';
    /** Lifetime of the session cookie, in seconds. */
    maxAgeSeconds: number;
  };
  /** Reverse proxy routes. */
  proxies: ProxyRoute[];
  /** Directory of the built SPA. */
  staticDir: string;
  /** SPA configuration served at `/config.json`, with `__APP_ENV__` substituted. */
  configJsonPath: string;
  /** Cache headers of the static files. */
  cache: {
    /** `Cache-Control` of `index.html` and `config.json`. */
    html: string;
    /** `Cache-Control` of the fingerprinted build output served under `/assets/`. */
    assets: string;
  };
}
