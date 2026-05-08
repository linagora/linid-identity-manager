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

import type { MenuItem } from '@linagora/linid-im-front-corelib';

/**
 * Whitelist of dotted lifecycle action keys exposed in the dropdown.
 *
 * The constant is the source of truth for the union {@link AccountLifecycleAction}
 * and for the runtime guard applied when the federated DropdownButton emits a
 * click.
 */
export const ACCOUNT_LIFECYCLE_ACTIONS = [
  'activation.immediate',
  'activation.scheduled',
  'suspension.immediate',
  'suspension.scheduled',
  'suspension.modify',
  'deactivation.immediate',
  'deactivation.scheduled',
  'deactivation.modify',
  'reactivation.immediate',
] as const;

/**
 * Union of all valid lifecycle action keys exposed in the dropdown.
 * Derived from {@link ACCOUNT_LIFECYCLE_ACTIONS} so the runtime whitelist
 * and the compile-time type stay in sync automatically.
 */
export type AccountLifecycleAction = (typeof ACCOUNT_LIFECYCLE_ACTIONS)[number];

/**
 * UI projection of the account lifecycle state, derived deterministically from
 * an {@link AccountStatus}. All template logic must consume this object only,
 * to avoid recomputing the same rules in multiple places.
 */
export interface AccountLifecycleUi {
  /**
   * Badge to display, or undefined when a banner replaces the badge.
   */
  showBadge?: boolean;
  /**
   * True when the yellow suspension banner must be rendered.
   */
  showSuspendedBanner?: boolean;
  /**
   * True when the light red deactivation warning banner must be rendered.
   */
  showDeactivationWarningBanner?: boolean;
  /**
   * True when the red "will be deactivated" info text must be rendered.
   */
  showWillDeactivateInfoText?: boolean;
  /**
   * True when the yellow "will be suspended" info text must be rendered.
   */
  showWillSuspendInfoText?: boolean;
  /**
   * True when the "user has not activated their account yet" info text must
   * be rendered.
   */
  showNotActivatedInfoText?: boolean;
  /**
   * Menu items consumed by the federated DropdownButton component, grouped by
   * action family (activation, suspension, deactivation, reactivation).
   * Undefined when no actions apply to the current state.
   */
  menuItems?: MenuItem[];
}

/**
 * Options accepted by {@link useAccountLifecycleUi}.
 */
export interface UseAccountLifecycleUiOptions {
  /**
   * Clock used as reference time when comparing validity and suspension
   * periods. Defaults to a function returning the current `Date`. Override to
   * project the lifecycle relative to a different instant (for example to
   * preview the state at a future date or align on a server clock).
   */
  now?: () => Date;
}
