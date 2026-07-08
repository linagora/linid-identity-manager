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
 * Raw application data transfer object as returned by `GET /applications/{id}`.
 *
 * The fields mirror the backend application view, where `createdBy` and `updatedBy` are resolved to human-readable full
 * names.
 */
export interface ApplicationDTO {
  /** Unique application identifier. */
  id: string;
  /** Functional code uniquely identifying the application. */
  code: string;
  /** Human-readable application name. */
  name: string;
  /** Free-text description of the application, when provided. */
  description?: string;
  /** Application protocol type, for example "OIDC". */
  type: string;
  /** Template used to build the token claims for the application. */
  claimsTemplate: string;
  /** ISO 8601 date-time when the application policy was last deployed, or null if never deployed. */
  deployedAt: string | null;
  /** Application configuration serialized as a JSON string. */
  configuration: string;
  /** Codes of the roles exposed by the application. */
  roles: string[];
  /** Full name of the user who created the application. */
  createdBy: string;
  /** Full name of the user who last updated the application. */
  updatedBy: string;
  /** Application creation timestamp in ISO 8601 / RFC 3339 UTC format. */
  insertDate: string;
  /** Application last update timestamp in ISO 8601 / RFC 3339 UTC format. */
  updateDate: string;
}
