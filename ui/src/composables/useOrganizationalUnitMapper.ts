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

import { useCommonMapper } from 'src/composables/useCommonMapper';
import type {
  OrganizationalUnit,
  OrganizationalUnitDTO,
  OrganizationalUnitForm,
  OrganizationalUnitReactivationRecord,
  OrganizationalUnitRecord,
  OrganizationalUnitStatus,
  OrganizationalUnitStatusForm,
  OrganizationalUnitSuspensionRecord,
} from 'src/types/organizationalUnits';

/**
 * Composable providing utility functions to work with organizational units: convert form values to API records, and
 * project an {@link OrganizationalUnitDTO} into identity / status views.
 *
 * @returns An object containing the mapping functions for organizational units.
 */
export function useOrganizationalUnitMapper() {
  const { toDate, toDateISO } = useCommonMapper();

  /**
   * Transforms an {@link OrganizationalUnitForm} into an {@link OrganizationalUnitRecord} by attaching the parent
   * identifier supplied by the navigation context.
   *
   * @param form OU form carrying the user-editable fields.
   * @param parent UUID of the parent OU, provided by the route context.
   * @returns OU record ready to be posted to the backend.
   */
  const toOrganizationalUnitRecord = (
    form: OrganizationalUnitForm,
    parent: string
  ): OrganizationalUnitRecord => {
    return {
      parent,
      name: form.name,
      type: form.type,
    };
  };

  /**
   * Maps an {@link OrganizationalUnitDTO} to an {@link OrganizationalUnit}, exposing only the identity fields. Combine
   * with {@link toOrganizationalUnitStatus} when both identity and suspension state are needed.
   *
   * @param dto OrganizationalUnitDTO to project.
   * @returns Identity projection with locale-formatted dates.
   */
  const toOrganizationalUnit = (
    dto: OrganizationalUnitDTO
  ): OrganizationalUnit => {
    return {
      id: dto.id,
      name: dto.name,
      type: dto.type,
      createdBy: dto.createdBy,
      updatedBy: dto.updatedBy,
      insertDate: toDate(dto.insertDate),
      updateDate: toDate(dto.updateDate),
    };
  };

  /**
   * Maps an {@link OrganizationalUnitDTO} to an {@link OrganizationalUnitStatus}, exposing only the suspension lifecycle
   * fields as raw ISO strings.
   *
   * @param dto OrganizationalUnitDTO to project.
   * @returns Suspension status projection.
   */
  const toOrganizationalUnitStatus = (
    dto: OrganizationalUnitDTO
  ): OrganizationalUnitStatus => {
    return {
      suspensionPeriod: dto.suspensionPeriod,
      isSuspended: dto.isSuspended,
    };
  };

  /**
   * Converts an {@link OrganizationalUnitStatusForm} into an {@link OrganizationalUnitSuspensionRecord} ready to be sent
   * to the backend. Localized start / end dates are normalized to ISO 8601 strings; empty values collapse to `null`.
   *
   * @param form Flattened lifecycle form values.
   * @returns Suspension record with an ISO suspension period.
   */
  const toOrganizationalUnitSuspensionRecord = (
    form: OrganizationalUnitStatusForm
  ): OrganizationalUnitSuspensionRecord => {
    return {
      suspensionPeriod: {
        start: toDateISO(form.start) || null,
        end: toDateISO(form.end) || null,
      },
      reason: form.reason || '',
      subreason: form.subreason || '',
      comment: form.comment || null,
    };
  };

  /**
   * Converts an {@link OrganizationalUnitStatusForm} into an {@link OrganizationalUnitReactivationRecord} carrying the
   * mandatory comment.
   *
   * @param form Flattened lifecycle form values.
   * @returns Reactivation record ready to be sent to the backend.
   */
  const toOrganizationalUnitReactivationRecord = (
    form: OrganizationalUnitStatusForm
  ): OrganizationalUnitReactivationRecord => {
    return {
      comment: form.comment || '',
    };
  };

  /**
   * Flattens an {@link OrganizationalUnitStatus} into an {@link OrganizationalUnitStatusForm} to pre-fill the "modify"
   * dialog with the existing suspension period bounds. The reason / sub-reason / comment fields are intentionally left
   * empty: the read model no longer carries a generic status reason, and a modification expects a fresh justification.
   *
   * @param status The current organizational unit status providing the existing period bounds.
   * @returns The flattened form values with localized suspension period dates.
   */
  const toOrganizationalUnitStatusForm = (
    status: OrganizationalUnitStatus
  ): OrganizationalUnitStatusForm => {
    return {
      start: toDate(status.suspensionPeriod?.start) || null,
      end: toDate(status.suspensionPeriod?.end) || null,
    };
  };

  return {
    toOrganizationalUnitRecord,
    toOrganizationalUnit,
    toOrganizationalUnitStatus,
    toOrganizationalUnitSuspensionRecord,
    toOrganizationalUnitReactivationRecord,
    toOrganizationalUnitStatusForm,
  };
}
