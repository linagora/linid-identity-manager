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

import type { LinidAttributeConfiguration } from '@linagora/linid-im-front-corelib';
import type { AccountLifecycleAction } from './accountLifecycleUi';
import type { OrganizationalUnitLifecycleAction } from './organizationalUnitLifecycleUi';

/**
 * Runtime application configuration loaded from `public/config.json`. Holds deployment-specific values that must be
 * tunable without rebuilding the UI.
 */
export interface AppConfig {
  /**
   * Deployment environment identifier (e.g. 'DEV', 'INTEG', 'PREPROD', 'PROD').
   *
   * This value is injected at runtime and may come from server-side substitution (e.g. Nginx env injection). If the
   * value is missing or invalid, the application may fallback to a default environment such as 'DEV'.
   */
  environment: string;
  /** Internationalization configuration for the application. */
  i18n: {
    /** Array of supported language codes. */
    languages: string[];
    /** Default locale of the application. */
    locale: string;
  };
  /** Defines the delay(in minutes) applied when reactivating a suspended or inactive account. */
  immediateActionDelay: number;
  /** List of design files to be loaded for the UI. */
  designFiles: string[];

  // ─── Accounts ───────────────────────────────────────────────────────────────

  /**
   * Ordered list of account attribute keys displayed on the account details page. Only keys listed here appear in the
   * details card; the order is preserved.
   */
  accountDetailsFieldsOrder: string[];

  /**
   * Form field configurations for each account lifecycle action dialog, keyed by action identifier (e.g.
   * `"suspension.immediate"`, `"deactivation.scheduled"`). Each value is the ordered list of fields rendered inside the
   * dialog.
   */
  accountLifecycleFields: Record<
    AccountLifecycleAction,
    LinidAttributeConfiguration[]
  >;

  // ─── Organizational units ────────────────────────────────────────────────────

  /** Ordered list of field definitions rendered in the organizational unit creation form. */
  organizationalUnitCreationFields: LinidAttributeConfiguration[];

  /** Ordered list of OU attribute keys displayed on the organizational unit details page. */
  organizationalUnitDetailsFieldsOrder: string[];

  /**
   * Form field configurations for each OU lifecycle action dialog, keyed by action identifier (e.g.
   * `"suspension.immediate"`, `"suspension.scheduled"`).
   */
  organizationalUnitLifecycleFields: Record<
    OrganizationalUnitLifecycleAction,
    LinidAttributeConfiguration[]
  >;

  // ─── Applications ───────────────────────────────────────────────────────────

  /** Ordered list of field definitions rendered in the application role create / edit dialog. */
  applicationRoleFields: LinidAttributeConfiguration[];
}
