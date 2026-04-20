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

/**
 * Writable fields of an account, sent to the backend when creating an
 * account. Distinct from {@link AccountDTO}: a record carries only
 * client-provided values, with no server-managed metadata, and may grow
 * write-only fields that never appear in the DTO.
 */
export interface AccountRecord {
  /**
   * External business identifier.
   */
  externalId: string;
  /**
   * User last name.
   */
  lastname: string;
  /**
   * User first name.
   */
  firstname: string;
  /**
   * User email address.
   */
  email: string;
}

/**
 * Raw account shape returned by the API.
 */
export interface AccountDTO {
  /**
   * Unique account identifier.
   */
  id: string;
  /**
   * External business identifier.
   */
  externalId: string;
  /**
   * User last name.
   */
  lastname: string;
  /**
   * User first name.
   */
  firstname: string;
  /**
   * User email address.
   */
  email: string;
  /**
   * Creator identifier.
   */
  createdBy: string;
  /**
   * Last updater identifier.
   */
  updatedBy: string;
  /**
   * Account creation timestamp in ISO 8601 / RFC 3339 UTC format
   * with nanosecond precision.
   * Example: 2026-04-15T17:09:36.898493688Z.
   */
  insertDate: string;
  /**
   * Account last update timestamp in ISO 8601 / RFC 3339 UTC format
   * with nanosecond precision.
   * Example: 2026-04-15T17:09:36.898493688Z.
   */
  updateDate: string;
}

/**
 * Account shape consumed by Vue components.
 */
export interface Account {
  /**
   * Unique account identifier.
   */
  id: string;
  /**
   * External business identifier.
   */
  externalId: string;
  /**
   * User last name.
   */
  lastname: string;
  /**
   * User first name.
   */
  firstname: string;
  /**
   * User email address.
   */
  email: string;
  /**
   * Creator identifier.
   */
  createdBy: string;
  /**
   * Last updater identifier.
   */
  updatedBy: string;
  /**
   * Account creation date converted from API ISO timestamp.
   * Display formatting depends on the user's locale.
   */
  insertDate: string;
  /**
   * Account last update date converted from API ISO timestamp.
   * Display formatting depends on the user's locale.
   */
  updateDate: string;
}
