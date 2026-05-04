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
import { useCommonMapper } from 'src/mappers/commonMapper';
import type { AccountStatus } from 'src/types/accounts';
import { computed, type ComputedRef, type Ref } from 'vue';

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
 * Discrete actions exposed in the lifecycle dropdown, identified by a dotted
 * key.
 */
export type AccountLifecycleAction = (typeof ACCOUNT_LIFECYCLE_ACTIONS)[number];

/**
 * Badge variant, mapped to a colour and label by the badge component.
 */
export type AccountLifecycleBadge = 'active' | 'inactive';

/**
 * UI projection of the account lifecycle state, derived deterministically from
 * an {@link AccountStatus}. All template logic must consume this object only,
 * to avoid recomputing the same rules in multiple places.
 */
export interface AccountLifecycleUi {
  /**
   * Badge to display, or undefined when a banner replaces the badge.
   */
  badge?: AccountLifecycleBadge;
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

/**
 * Partial UI projection augmented with the ordered action list that drives the
 * dropdown menu items. Used internally by case branches so each one can
 * declare its actions flatly while letting `withMenuItems` translate them.
 */
interface PartialUiWithActions extends AccountLifecycleUi {
  /**
   * Ordered list of dotted action keys exposed in the dropdown.
   */
  actions?: AccountLifecycleAction[];
}

/**
 * Composable that projects an {@link AccountStatus} into a deterministic UI
 * state describing which badge, banners, info texts and dropdown menu items
 * must be displayed on the Account Details page, following the lifecycle
 * matrix defined by the product.
 *
 * The projection is the single source of truth for lifecycle rendering: no
 * other component should reimplement these rules.
 * @param accountStatus - Reactive reference to the account status, or null while loading.
 * @param options - Optional projection options, see {@link UseAccountLifecycleUiOptions}.
 * @returns A reactive UI projection, or null when the account is not loaded.
 */
export function useAccountLifecycleUi(
  accountStatus: Ref<AccountStatus | null>,
  options: UseAccountLifecycleUiOptions = {}
): ComputedRef<AccountLifecycleUi | null> {
  const { toDateObject } = useCommonMapper();

  /**
   * True when `daysBeforeDeactivation` is set and the deactivation falls
   * within the 15-day warning window (boundary inclusive).
   * @param status - The account lifecycle status.
   * @returns Whether the account is within the 15-day deactivation window.
   */
  function isWithin15Days(status: AccountStatus): boolean {
    return (
      status.daysBeforeDeactivation != null &&
      status.daysBeforeDeactivation <= 15
    );
  }

  /**
   * True when `daysBeforeDeactivation` is set and the deactivation is still
   * more than 15 days away.
   * @param status - The account lifecycle status.
   * @returns Whether the account has more than 15 days before deactivation.
   */
  function isMoreThan15Days(status: AccountStatus): boolean {
    return (
      status.daysBeforeDeactivation != null &&
      status.daysBeforeDeactivation > 15
    );
  }

  /**
   * True when a suspension is scheduled to start strictly after `now`.
   * @param status - The account lifecycle status.
   * @param now - The reference time used for time-based comparisons.
   * @returns Whether a future suspension is planned.
   */
  function hasFutureSuspension(status: AccountStatus, now: Date): boolean {
    const suspensionStart = toDateObject(status.suspensionPeriod?.start);
    return (
      suspensionStart !== null && suspensionStart.getTime() > now.getTime()
    );
  }

  /**
   * Builds the menu items exposed by the dropdown for the given ordered list
   * of actions. Items are grouped by action family (the segment before the
   * dot) so the federated DropdownButton renders one root entry per family
   * and one sub-entry per action.
   * @param actions - Ordered list of dotted action keys to expose.
   * @returns Menu items consumed by the federated DropdownButton component.
   */
  function buildMenuItems(actions: AccountLifecycleAction[]): MenuItem[] {
    const groups = new Map<string, string[]>();
    for (const action of actions) {
      const dotIndex = action.indexOf('.');
      const group = action.slice(0, dotIndex);
      const child = action.slice(dotIndex + 1);
      const list = groups.get(group);
      if (list) {
        if (!list.includes(child)) {
          list.push(child);
        }
      } else {
        groups.set(group, [child]);
      }
    }
    return Array.from(groups.entries()).map(([group, children]) => ({
      key: group,
      clickable: true,
      children,
    }));
  }

  /**
   * Wraps a partial UI projection by computing its `menuItems` from `actions`.
   * Lets every case branch declare its action list flatly without repeating
   * the menu-item construction.
   * @param ui - Partial UI projection. The `actions` field, when present, drives the dropdown menu items.
   * @returns The same projection with `menuItems` populated when actions exist.
   */
  function withMenuItems(ui: PartialUiWithActions): AccountLifecycleUi {
    const { actions, ...rest } = ui;
    if (actions === undefined || actions.length === 0) {
      return rest;
    }

    return { ...rest, menuItems: buildMenuItems(actions) };
  }

  /**
   * Projects an INACTIVE account whose validity start is still in the future.
   * @param status - The account lifecycle status.
   * @param now - The reference time used for time-based comparisons.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function futureActivation(
    status: AccountStatus,
    now: Date
  ): AccountLifecycleUi | undefined {
    const validityStart = toDateObject(status.validityPeriod?.start);
    if (
      status.status === 'INACTIVE' &&
      validityStart != null &&
      validityStart.getTime() > now.getTime()
    ) {
      return withMenuItems({
        badge: 'inactive',
        actions: [
          'activation.immediate',
          'activation.scheduled',
          'suspension.scheduled',
          'deactivation.scheduled',
        ],
      });
    }
  }

  /**
   * Projects an INACTIVE account that has never been activated.
   * @param status - The account lifecycle status.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function notActivatedYet(
    status: AccountStatus
  ): AccountLifecycleUi | undefined {
    const activationAt = toDateObject(status.activationAt);
    if (activationAt == null && status.status === 'INACTIVE') {
      return withMenuItems({
        badge: 'inactive',
        showNotActivatedInfoText: true,
        actions: ['suspension.scheduled', 'deactivation.scheduled'],
      });
    }
  }

  /**
   * Projects an ACTIVE account with no end date and no future suspension.
   * @param status - The account lifecycle status.
   * @param now - The reference time used for time-based comparisons.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function activeWithoutEndDate(
    status: AccountStatus,
    now: Date
  ): AccountLifecycleUi | undefined {
    if (
      status.status === 'ACTIVE' &&
      status.validityPeriod?.end == null &&
      !hasFutureSuspension(status, now)
    ) {
      return withMenuItems({
        badge: 'active',
        actions: [
          'suspension.immediate',
          'suspension.scheduled',
          'deactivation.immediate',
          'deactivation.scheduled',
        ],
      });
    }
  }

  /**
   * Projects an ACTIVE account with more than 15 days before deactivation and
   * no future suspension.
   * @param status - The account lifecycle status.
   * @param now - The reference time used for time-based comparisons.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function activeMoreThan15Days(
    status: AccountStatus,
    now: Date
  ): AccountLifecycleUi | undefined {
    if (
      status.status === 'ACTIVE' &&
      isMoreThan15Days(status) &&
      !hasFutureSuspension(status, now)
    ) {
      return withMenuItems({
        badge: 'active',
        showWillDeactivateInfoText: true,
        actions: [
          'suspension.immediate',
          'suspension.scheduled',
          'deactivation.immediate',
          'deactivation.modify',
        ],
      });
    }
  }

  /**
   * Projects an ACTIVE account with 15 days or less before deactivation and
   * no future suspension.
   * @param status - The account lifecycle status.
   * @param now - The reference time used for time-based comparisons.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function activeWithin15Days(
    status: AccountStatus,
    now: Date
  ): AccountLifecycleUi | undefined {
    if (
      status.status === 'ACTIVE' &&
      isWithin15Days(status) &&
      !hasFutureSuspension(status, now)
    ) {
      return withMenuItems({
        badge: 'active',
        showDeactivationWarningBanner: true,
        actions: ['suspension.immediate', 'suspension.scheduled'],
      });
    }
  }

  /**
   * Projects an ACTIVE account with no end date and a future suspension.
   * @param status - The account lifecycle status.
   * @param now - The reference time used for time-based comparisons.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function activeWithoutEndDateAndFutureSuspension(
    status: AccountStatus,
    now: Date
  ): AccountLifecycleUi | undefined {
    if (
      status.status === 'ACTIVE' &&
      status.validityPeriod?.end == null &&
      hasFutureSuspension(status, now)
    ) {
      return withMenuItems({
        badge: 'active',
        showWillSuspendInfoText: true,
        actions: [
          'suspension.immediate',
          'suspension.scheduled',
          'deactivation.immediate',
          'deactivation.scheduled',
        ],
      });
    }
  }

  /**
   * Projects an ACTIVE account with more than 15 days before deactivation and
   * a future suspension.
   * @param status - The account lifecycle status.
   * @param now - The reference time used for time-based comparisons.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function activeMoreThan15DaysWithFutureSuspension(
    status: AccountStatus,
    now: Date
  ): AccountLifecycleUi | undefined {
    if (
      status.status === 'ACTIVE' &&
      isMoreThan15Days(status) &&
      hasFutureSuspension(status, now)
    ) {
      return withMenuItems({
        badge: 'active',
        showWillDeactivateInfoText: true,
        showWillSuspendInfoText: true,
        actions: [
          'suspension.immediate',
          'suspension.scheduled',
          'deactivation.immediate',
          'deactivation.modify',
        ],
      });
    }
  }

  /**
   * Projects an ACTIVE account with 15 days or less before deactivation and
   * a future suspension.
   * @param status - The account lifecycle status.
   * @param now - The reference time used for time-based comparisons.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function activeWithin15DaysWithFutureSuspension(
    status: AccountStatus,
    now: Date
  ): AccountLifecycleUi | undefined {
    if (
      status.status === 'ACTIVE' &&
      isWithin15Days(status) &&
      hasFutureSuspension(status, now)
    ) {
      return withMenuItems({
        badge: 'active',
        showDeactivationWarningBanner: true,
        showWillSuspendInfoText: true,
        actions: ['suspension.immediate', 'suspension.scheduled'],
      });
    }
  }

  /**
   * Projects a SUSPENDED account with no validity end and no suspension end.
   * @param status - The account lifecycle status.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function suspendedWithoutEndDateAndSuspensionEnd(
    status: AccountStatus
  ): AccountLifecycleUi | undefined {
    if (
      status.status === 'SUSPENDED' &&
      status.validityPeriod?.end == null &&
      toDateObject(status.suspensionPeriod?.end) == null
    ) {
      return withMenuItems({
        showSuspendedBanner: true,
        actions: ['deactivation.immediate', 'deactivation.scheduled'],
      });
    }
  }

  /**
   * Projects a SUSPENDED account with no validity end but a suspension end.
   * @param status - The account lifecycle status.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function suspendedWithoutEndDateAndWithSuspensionEnd(
    status: AccountStatus
  ): AccountLifecycleUi | undefined {
    if (
      status.status === 'SUSPENDED' &&
      status.validityPeriod?.end == null &&
      toDateObject(status.suspensionPeriod?.end) != null
    ) {
      return withMenuItems({
        showSuspendedBanner: true,
        actions: ['deactivation.immediate', 'deactivation.scheduled'],
      });
    }
  }

  /**
   * Projects a SUSPENDED account more than 15 days away from deactivation.
   * @param status - The account lifecycle status.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function suspendedMoreThan15DaysBeforeDeactivation(
    status: AccountStatus
  ): AccountLifecycleUi | undefined {
    if (status.status === 'SUSPENDED' && isMoreThan15Days(status)) {
      return withMenuItems({
        showSuspendedBanner: true,
        showWillDeactivateInfoText: true,
        actions: ['deactivation.immediate', 'deactivation.scheduled'],
      });
    }
  }

  /**
   * Projects a SUSPENDED account 15 days or less away from deactivation.
   * @param status - The account lifecycle status.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function suspendedWithin15DaysBeforeDeactivation(
    status: AccountStatus
  ): AccountLifecycleUi | undefined {
    if (status.status === 'SUSPENDED' && isWithin15Days(status)) {
      return withMenuItems({
        showSuspendedBanner: true,
        showDeactivationWarningBanner: true,
        actions: ['deactivation.immediate', 'deactivation.scheduled'],
      });
    }
  }

  return computed(() => {
    const value = accountStatus.value;
    if (value === null || value === undefined || !value.status) {
      return null;
    }
    const now = options.now?.() ?? new Date();
    return (
      futureActivation(value, now) ??
      notActivatedYet(value) ??
      activeWithin15DaysWithFutureSuspension(value, now) ??
      activeMoreThan15DaysWithFutureSuspension(value, now) ??
      activeWithoutEndDateAndFutureSuspension(value, now) ??
      activeWithin15Days(value, now) ??
      activeMoreThan15Days(value, now) ??
      activeWithoutEndDate(value, now) ??
      suspendedWithin15DaysBeforeDeactivation(value) ??
      suspendedMoreThan15DaysBeforeDeactivation(value) ??
      suspendedWithoutEndDateAndWithSuspensionEnd(value) ??
      suspendedWithoutEndDateAndSuspensionEnd(value) ??
      {}
    );
  });
}
