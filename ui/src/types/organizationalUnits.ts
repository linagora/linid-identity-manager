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

import type { Period } from 'src/types/common';

/** Relation between an organizational unit and one of its parents in the DAG. */
export interface OrganizationalUnitRelationDTO {
  /** Unique identifier of the relation. */
  id: string;
  /** Unique identifier of the parent organizational unit. */
  parent: string;
}

/**
 * Writable fields of an organizational unit, sent to the backend when creating a new OU. Distinct from
 * {@link OrganizationalUnitDTO}: a record carries only client-provided values, with no server-managed metadata.
 */
export interface OrganizationalUnitRecord {
  /**
   * Identifier of the parent organizational unit. Always required: the root is the only OU without a parent and is
   * created by the backend.
   */
  parent: string;
  /** Human-readable name of the organizational unit. */
  name: string;
  /** Type of the organizational unit, picked from a fixed list of values. */
  type: string;
}

/** Raw organizational unit shape returned by the API. */
export interface OrganizationalUnitDTO {
  /** Unique identifier of the organizational unit. */
  id: string;
  /** Human-readable name of the organizational unit. */
  name: string;
  /** Type of the organizational unit. */
  type: string;
  /** Creator identifier. */
  createdBy: string;
  /** Last updater identifier. */
  updatedBy: string;
  /** Organizational unit creation timestamp in ISO 8601 format. */
  insertDate: string;
  /** Organizational unit last update timestamp in ISO 8601 format. */
  updateDate: string;
  /** Period during which the organizational unit is suspended. Null when no suspension is configured. */
  suspensionPeriod: Period | null;
  /**
   * Whether the organizational unit is currently suspended (server-computed from `suspensionPeriod` against the current
   * instant).
   */
  isSuspended: boolean;
  /** List of parent organizational units, with their identifiers and relation IDs. */
  parents?: OrganizationalUnitRelationDTO[];
}

/**
 * Identity projection of an organizational unit consumed by Vue components on the Details page. Identity fields only;
 * combine with {@link OrganizationalUnitStatus} when both identity and suspension state are needed.
 */
export interface OrganizationalUnit {
  /** Unique identifier of the organizational unit. */
  id: string;
  /** Human-readable name of the organizational unit. */
  name: string;
  /** Type of the organizational unit. */
  type: string;
  /** Creator identifier. */
  createdBy: string;
  /** Last updater identifier. */
  updatedBy: string;
  /** Organizational unit creation date converted from API ISO timestamp. */
  insertDate: string;
  /** Organizational unit last update date converted from API ISO timestamp. */
  updateDate: string;
}

/**
 * Suspension status fields of an organizational unit: suspension period, reason metadata, and the computed
 * `isSuspended` flag.
 *
 * Combine with an {@link OrganizationalUnit} when both identity and lifecycle data are needed (for example on the OU
 * Details page).
 */
export interface OrganizationalUnitStatus {
  /** Period during which the organizational unit is suspended. Null when no suspension is configured. */
  suspensionPeriod: Period | null;
  /** Whether the organizational unit is currently suspended. */
  isSuspended: boolean;
}

/**
 * Payload for `PUT /organizational-units/{id}/status`. The backend currently requires `suspensionPeriod` to be non-null
 * (a future-only range).
 */
export interface OrganizationalUnitSuspensionRecord {
  /** Suspension period (start mandatory, end optional). Backend rejects a `start` strictly in the past. */
  suspensionPeriod: Period;
  /** High-level reason code. */
  reason: string;
  /** Detailed classification of the reason. */
  subreason: string;
  /** Free-text comment. */
  comment?: string | null;
}

/** Shape of the payload sent to the backend when reactivating an organizational unit. */
export interface OrganizationalUnitReactivationRecord {
  /** Mandatory justification for the reactivation. */
  comment: string;
}

/**
 * Flattened shape of the organizational unit lifecycle dialogs. Each field maps to a single form control rendered by
 * the shared form dialog; the suspension period is split into individual `start` / `end` values that are converted back
 * into an {@link OrganizationalUnitStatusRecord} before being sent to the API.
 */
export interface OrganizationalUnitStatusForm {
  /** Localized suspension start date, or an ISO string for immediate actions. */
  start?: string | null;
  /** Localized suspension end date. */
  end?: string | null;
  /** High-level reason code. */
  reason?: string | null;
  /** Detailed classification of the reason. */
  subreason?: string | null;
  /** Free-text comment. */
  comment?: string | null;
}

/**
 * Shape of the OU creation form. The `parent` value is provided by the navigation context and is therefore not present
 * here; the form only carries user-editable fields.
 */
export interface OrganizationalUnitForm {
  /** Human-readable name of the organizational unit. */
  name: string;
  /** Type of the organizational unit, picked from a fixed list of values. */
  type: string;
}
