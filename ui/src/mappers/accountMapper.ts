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

import { useCommonMapper } from 'src/mappers/commonMapper';
import type {
  Account,
  AccountDTO,
  AccountForm,
  AccountQueryFilterDTO,
  AccountRecord,
  AccountStatus,
} from 'src/types/accounts';

/**
 * Mapper for accounts-related data transformations.
 * @returns Functions to convert API records to UI-friendly formats and to transform search filters for Spring Security.
 */
export function useAccountMapper() {
  const {
    toDate,
    toDateFilter,
    toDateISO,
    toLikeFilter,
    SPRING_QUERY_DATE_FORMAT,
  } = useCommonMapper();

  /**
   * Maps an AccountDTO to an Account, converting date to date ISO.
   * @param account AccountDTO to be transformed into an Account.
   * @returns Account with properly typed fields for UI.
   */
  const toAccount = (account: AccountDTO): Account => {
    return {
      id: account.id,
      externalId: account.externalId,
      lastname: account.lastname,
      firstname: account.firstname,
      email: account.email,
      createdBy: account.createdBy,
      updatedBy: account.updatedBy,
      insertDate: toDate(account.insertDate),
      updateDate: toDate(account.updateDate),
    };
  };

  /**
   * Maps an AccountDTO to an AccountStatus, exposing only the lifecycle status
   * fields. Identity fields (firstname, lastname, ...) are intentionally not
   * included; combine with {@link toAccount} when both are needed.
   * @param account AccountDTO to be transformed into an AccountStatus.
   * @returns AccountStatus with lifecycle fields preserved as ISO strings.
   */
  const toAccountStatus = (account: AccountDTO): AccountStatus => {
    return {
      status: account.status,
      validityPeriod: account.validityPeriod,
      suspensionPeriod: account.suspensionPeriod,
      activationAt: account.activationAt,
      statusReason: account.statusReason,
      statusSubreason: account.statusSubreason,
      statusComment: account.statusComment,
      daysBeforeDeactivation: account.daysBeforeDeactivation,
    };
  };

  /**
   * Maps an array of AccountDTOs to an array of Accounts, converting dates from date ISO to date with local date format.
   * @param accounts Array of AccountDTOs to be transformed into Accounts.
   * @returns Array of Accounts with properly typed fields for UI.
   */
  const toAccountList = (accounts: AccountDTO[]): Account[] => {
    return accounts.map(toAccount);
  };

  /**
   * Transforms advanced search filters from the UI into a format suitable for API filters. The `insertDate` value is forwarded as-is: the date field applies a locale-aware input mask, so the value is already formatted in the same `dateFormat` declared in the i18n translations.
   * @param advancedSearchFilters Record containing the raw filter values entered by the user in the advanced search form.
   * @returns AccountQueryFilterDTO with properly formatted filter values for querying accounts in the backend.
   */
  const toAccountQueryFilterDTO = (
    advancedSearchFilters: Record<string, unknown>
  ): AccountQueryFilterDTO => {
    return {
      lastname: toLikeFilter(advancedSearchFilters['lastname']),
      firstname: toLikeFilter(advancedSearchFilters['firstname']),
      email: toLikeFilter(advancedSearchFilters['email']),
      createdBy: toLikeFilter(advancedSearchFilters['createdBy']),
      insertDate: toDateFilter(advancedSearchFilters['insertDate']),
      dateFormat: SPRING_QUERY_DATE_FORMAT,
    };
  };

  /**
   * Transforms an AccountForm into an AccountRecord, nesting the validity period fields into a single validityPeriod object.
   * @param account AccountForm containing the flat fields from the account creation form.
   * @returns AccountRecord with the validity period properly structured for API consumption.
   */
  const toAccountRecord = (account: AccountForm): AccountRecord => {
    const { validityPeriodStart, ...baseFields } = account;

    return {
      ...baseFields,
      validityPeriod: {
        start: toDateISO(validityPeriodStart) || null,
        end: null,
      },
    };
  };

  return {
    toAccount,
    toAccountStatus,
    toAccountList,
    toAccountQueryFilterDTO,
    toAccountRecord,
  };
}
