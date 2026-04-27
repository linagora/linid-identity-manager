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

import type { AccountStatus } from 'src/types/accounts';

/**
 * Props for all AccountBanner components.
 */
export interface AccountBannerProps {
  /**
   * Current status of the account (e.g. Active, deactivated, suspended).
   */
  accountStatus: AccountStatus;
}

/**
 * Event outputs emitted by the Account Deactivated Banner component.
 *
 * Defines user actions related to account reactivation flows.
 */
export interface AccountDeactivatedBannerOutputs {
  /**
   * Triggered when the user chooses to immediately reactivate their account.
   */
  'reactivate-immediate': [];

  /**
   * Triggered when the user schedules a future account reactivation.
   */
  'reactivate-scheduled': [];
}

/**
 * Events emitted by the Account Deactivated Warning Banner component.
 *
 * Represents user actions related to managing account deactivation and reactivation flows.
 */
export interface AccountDeactivatedWarningBannerOutputs {
  /**
   * Emitted when the user chooses to immediately deactivate their account.
   */
  'deactivate-immediate': [];

  /**
   * Emitted when the user requests to modify the scheduled deactivation date.
   */
  'modify-deactivation': [];
}

/**
 * Events emitted by the Account Suspended Banner component.
 *
 * Represents user actions related to managing account suspension and recovery flows.
 */
export interface AccountSuspendedBannerOutputs {
  /**
   * Emitted when the user chooses to immediately clear the account suspension.
   */
  'clear-suspension': [];

  /**
   * Emitted when the user requests to modify the suspension parameters or schedule.
   */
  'modify-suspension': [];
}
