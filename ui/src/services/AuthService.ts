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
  UserManager,
  WebStorageStateStore,
  type User,
  type UserManagerSettings,
} from 'oidc-client-ts';

/** OpenID Connect configuration loaded from `/oidc-config.json`. */
interface OidcConfig {
  /** URL of the OpenID Connect authority (Identity Provider). */
  authority: string;

  /** OAuth 2.0 / OIDC client identifier. */
  client_id: string;

  /** URI where the Identity Provider redirects the user after a successful login. */
  redirect_uri: string;

  /** URI where the Identity Provider redirects the user after logout. */
  post_logout_redirect_uri?: string;

  /** OAuth 2.0 response type used during authentication. Typically set to `code`. */
  response_type?: string;

  /** Requested OAuth 2.0/OIDC scopes. */
  scope?: string;

  /** URI used for silent token renewal. */
  silent_redirect_uri?: string;

  /** Additional OIDC configuration properties. */
  [key: string]: unknown;
}

/** Service responsible for authentication and session management using OpenID Connect (OIDC). */
class AuthService {
  private userManager: UserManager | null = null;
  private initPromise: Promise<UserManager> | null = null;
  private loginPromise: Promise<void> | null = null;

  /**
   * Initializes the OIDC client and creates the underlying {@link UserManager} instance.
   *
   * Multiple calls return the same initialization promise.
   *
   * @returns A promise resolving to the configured {@link UserManager}.
   */
  async init(): Promise<UserManager> {
    if (this.initPromise) {
      return this.initPromise;
    }

    this.initPromise = this.buildUserManager();
    return this.initPromise;
  }

  /**
   * Loads the OIDC configuration and creates a configured {@link UserManager} instance.
   *
   * This method also registers authentication event handlers for token expiration, silent renewal failures, and remote
   * sign-out.
   *
   * @returns A promise resolving to the configured {@link UserManager}.
   * @throws {Error} If the OIDC configuration file cannot be loaded.
   */
  private async buildUserManager(): Promise<UserManager> {
    const response = await fetch('/oidc-config.json');
    if (!response.ok) {
      throw new Error(`Unable to load /oidc-config.json (${response.status})`);
    }
    const config: OidcConfig = await response.json();

    const settings: UserManagerSettings = {
      authority: config.authority,
      client_id: config.client_id,
      redirect_uri: config.redirect_uri,
      post_logout_redirect_uri: config.post_logout_redirect_uri,
      silent_redirect_uri: config.silent_redirect_uri,
      response_type: config.response_type,
      scope: config.scope,
      automaticSilentRenew: true,
      loadUserInfo: true,
      userStore: new WebStorageStateStore({ store: window.localStorage }),
    };

    const userManager = new UserManager(settings);

    userManager.events.addAccessTokenExpired(() => {
      console.warn('[auth] access token expired, redirecting to login');
      void this.login();
    });

    userManager.events.addSilentRenewError((err) => {
      console.error('[auth] silent renew failed', err);
      void this.login();
    });

    userManager.events.addUserSignedOut(() => {
      void this.login();
    });

    this.userManager = userManager;
    return userManager;
  }

  /**
   * Returns the initialized {@link UserManager}.
   *
   * @returns The configured user manager instance.
   * @throws {Error} If the service has not been initialized.
   */
  private getManager(): UserManager {
    if (!this.userManager) {
      throw new Error('AuthService not initialized, call init() first');
    }
    return this.userManager;
  }

  /**
   * Retrieves the currently authenticated user.
   *
   * @returns The authenticated user, or `null` if no valid session exists or the access token has expired.
   */
  async getUser(): Promise<User | null> {
    const manager = this.getManager();
    const user = await manager.getUser();
    if (!user || user.expired) {
      return null;
    }
    return user;
  }

  /**
   * Initiates the OIDC authentication flow by redirecting the user to the Identity Provider.
   *
   * The current application route is stored in the OIDC state and can be used to restore navigation after successful
   * authentication.
   *
   * This method is protected against concurrent invocations: if a login flow is already in progress, the existing
   * promise is returned instead of triggering a new redirect.
   *
   * @remarks
   *   The returned promise does not represent a post-login completion. The browser will typically be redirected away
   *   before resolution context is useful.
   * @param url Optional return URL to store in the authentication state. If not provided, the current path and query
   *   string are used.
   * @returns A promise that resolves once the redirect request has been initiated.
   */
  async login(url?: string): Promise<void> {
    if (this.loginPromise) {
      return this.loginPromise;
    }

    const manager = this.getManager();

    this.loginPromise = (async () => {
      const redirectUrl =
        url ?? window.location.pathname + window.location.search;

      try {
        await manager.signinRedirect({
          state: { redirectUrl },
        });
      } finally {
        this.loginPromise = null;
      }
    })();

    return this.loginPromise;
  }

  /**
   * Processes the authentication callback returned by the Identity Provider after a successful login.
   *
   * @returns The authenticated user.
   */
  async handleCallback(): Promise<User> {
    const manager = this.getManager();
    return manager.signinRedirectCallback();
  }

  /**
   * Processes the silent token renewal callback.
   *
   * This method should be invoked from the page configured as the silent renewal redirect URI.
   *
   * @returns A promise that resolves when the callback has been processed.
   */
  async handleSilentRenewCallback(): Promise<void> {
    const manager = this.getManager();
    await manager.signinSilentCallback();
  }

  /**
   * Starts the logout flow by redirecting the user to the Identity Provider logout endpoint.
   *
   * @returns A promise that resolves once the logout redirect has been initiated.
   */
  async logout(): Promise<void> {
    const manager = this.getManager();
    await manager.signoutRedirect();
  }

  /**
   * Returns the current access token.
   *
   * @returns The access token if a valid user session exists, otherwise `null`.
   */
  async getAccessToken(): Promise<string | null> {
    const user = await this.getUser();
    return user?.access_token ?? null;
  }
}

export const authService = new AuthService();
