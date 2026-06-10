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
import { useCommonMapper } from 'src/composables/useCommonMapper';
import type {
  AccountLifecycleAction,
  AccountLifecycleUi,
} from 'src/types/accountLifecycleUi';
import type { AccountStatus } from 'src/types/accounts';
import { computed, type ComputedRef, type Ref } from 'vue';

/**
 * Composable that projects an {@link AccountStatus} into a deterministic UI
 * state describing which badge, banners, info texts and dropdown menu items
 * must be displayed on the Account Details page, following the lifecycle
 * matrix defined by the product.
 *
 * The projection is the single source of truth for lifecycle rendering: no
 * other component should reimplement these rules.
 * @param accountStatus - Reactive reference to the account status, or null while loading.
 * @returns A reactive UI projection, or null when the account is not loaded.
 */
export function useAccountLifecycleUi(
  accountStatus: Ref<AccountStatus | null>
): ComputedRef<AccountLifecycleUi | null> {
  const { toDayJs } = useCommonMapper();

  /**
   * True when `daysBeforeDeactivation` is set and the deactivation falls
   * within the 15-day warning window (boundary inclusive).
   * @param status - The account status.
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
   * @param status - The account status.
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
   * @param status - The account status.
   * @param now - The reference time used for time-based comparisons.
   * @returns Whether a future suspension is planned.
   */
  function hasFutureSuspension(status: AccountStatus, now: Date): boolean {
    const suspensionStart = toDayJs(status.suspensionPeriod?.start);
    return suspensionStart !== null && suspensionStart.isAfter(now);
  }

  /**
   * Result of {@link toMenuItemsByGroup}, exposing one optional menu items
   * list per action group. Missing properties indicate empty groups.
   */
  interface MenuItemsByGroup {
    /** Menu items for the activation dropdown. */
    activation?: MenuItem[];
    /** Menu items for the suspension dropdown. */
    suspension?: MenuItem[];
    /** Menu items for the deactivation dropdown. */
    deactivation?: MenuItem[];
  }

  /**
   * Builds the per-dropdown menu items from the given ordered list of actions.
   * Actions are dispatched into three groups (activation, suspension,
   * deactivation) so the page renders one dropdown per group with its own
   * label and colour. Reactivation actions are grouped under activation.
   * @param actions - Ordered list of dotted action keys to expose.
   * @returns Menu items split by group; missing entries when a group is empty.
   */
  function toMenuItemsByGroup(
    actions: AccountLifecycleAction[]
  ): MenuItemsByGroup {
    const groups = new Map<
      'activation' | 'suspension' | 'deactivation',
      MenuItem[]
    >();
    for (const action of actions) {
      const prefix = action.split('.')[0] as string;
      const group =
        prefix === 'reactivation'
          ? 'activation'
          : (prefix as 'activation' | 'suspension' | 'deactivation');
      const items = groups.get(group) ?? [];
      items.push({ key: action, clickable: true });
      groups.set(group, items);
    }
    return {
      activation: groups.get('activation'),
      suspension: groups.get('suspension'),
      deactivation: groups.get('deactivation'),
    };
  }

  /**
   * Helper that builds the UI projection of a case from its base flags and
   * its ordered action list.
   * @param base - Visual flags (badge, banners, info texts) for this case.
   * @param actions - Ordered list of dotted action keys.
   * @returns The full UI projection with the three dropdown menu item lists.
   */
  function withActions(
    base: AccountLifecycleUi,
    actions: AccountLifecycleAction[]
  ): AccountLifecycleUi {
    const { activation, suspension, deactivation } =
      toMenuItemsByGroup(actions);
    return {
      ...base,
      activationMenuItems: activation,
      suspensionMenuItems: suspension,
      deactivationMenuItems: deactivation,
    };
  }

  /**
   * Projects an INACTIVE account whose validity start is still in the future.
   * @param status - The account status.
   * @param now - The reference time used for time-based comparisons.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function futureActivation(
    status: AccountStatus,
    now: Date
  ): AccountLifecycleUi | undefined {
    const validityStart = toDayJs(status.validityPeriod?.start);
    if (
      status.status === 'INACTIVE' &&
      validityStart != null &&
      validityStart.isAfter(now)
    ) {
      return withActions({ showBadge: true }, [
        'activation.immediate',
        'activation.scheduled',
        'suspension.scheduled',
        'deactivation.scheduled',
      ]);
    }
  }

  /**
   * Projects an INACTIVE account that has never been activated.
   * @param status - The account status.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function notActivatedYet(
    status: AccountStatus
  ): AccountLifecycleUi | undefined {
    const activationAt = toDayJs(status.activationAt);
    if (activationAt == null && status.status === 'INACTIVE') {
      return withActions({ showBadge: true, showNotActivatedInfoText: true }, [
        'suspension.scheduled',
        'deactivation.scheduled',
      ]);
    }
  }

  /**
   * Projects an INACTIVE account that was activated but whose validity period
   * end is now in the past: the account is deactivated. Surfaces the red
   * deactivated banner whose actions re-validate the account immediately or
   * schedule a new validity end.
   * @param status - The account status.
   * @param now - The reference time used for time-based comparisons.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function deactivated(
    status: AccountStatus,
    now: Date
  ): AccountLifecycleUi | undefined {
    const validityEnd = toDayJs(status.validityPeriod?.end);
    if (
      status.status === 'INACTIVE' &&
      toDayJs(status.activationAt) != null &&
      validityEnd != null &&
      validityEnd.isBefore(now)
    ) {
      return withActions({ showBadge: true, showDeactivatedBanner: true }, []);
    }
  }

  /**
   * Projects an ACTIVE account with no end date and no future suspension.
   * @param status - The account status.
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
      return withActions({ showBadge: true }, [
        'suspension.immediate',
        'suspension.scheduled',
        'deactivation.immediate',
        'deactivation.scheduled',
      ]);
    }
  }

  /**
   * Projects an ACTIVE account with more than 15 days before deactivation and
   * no future suspension.
   * @param status - The account status.
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
      return withActions(
        { showBadge: true, showWillDeactivateInfoText: true },
        [
          'suspension.immediate',
          'suspension.scheduled',
          'deactivation.immediate',
          'deactivation.modify',
        ]
      );
    }
  }

  /**
   * Projects an ACTIVE account with 15 days or less before deactivation and
   * no future suspension.
   * @param status - The account status.
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
      return withActions(
        { showBadge: true, showDeactivationWarningBanner: true },
        ['suspension.immediate', 'suspension.scheduled']
      );
    }
  }

  /**
   * Projects an ACTIVE account with no end date and a future suspension.
   * @param status - The account status.
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
      return withActions({ showBadge: true, showWillSuspendInfoText: true }, [
        'suspension.immediate',
        'suspension.scheduled',
        'deactivation.immediate',
        'deactivation.scheduled',
      ]);
    }
  }

  /**
   * Projects an ACTIVE account with more than 15 days before deactivation and
   * a future suspension.
   * @param status - The account status.
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
      return withActions(
        {
          showBadge: true,
          showWillDeactivateInfoText: true,
          showWillSuspendInfoText: true,
        },
        [
          'suspension.immediate',
          'suspension.scheduled',
          'deactivation.immediate',
          'deactivation.modify',
        ]
      );
    }
  }

  /**
   * Projects an ACTIVE account with 15 days or less before deactivation and
   * a future suspension.
   * @param status - The account status.
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
      return withActions(
        {
          showBadge: true,
          showDeactivationWarningBanner: true,
          showWillSuspendInfoText: true,
        },
        ['suspension.immediate', 'suspension.scheduled']
      );
    }
  }

  /**
   * Projects a SUSPENDED account with no validity end and no suspension end.
   * @param status - The account status.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function suspendedWithoutEndDateAndSuspensionEnd(
    status: AccountStatus
  ): AccountLifecycleUi | undefined {
    if (
      status.status === 'SUSPENDED' &&
      status.validityPeriod?.end == null &&
      toDayJs(status.suspensionPeriod?.end) == null
    ) {
      return withActions({ showSuspendedBanner: true }, [
        'deactivation.immediate',
        'deactivation.scheduled',
      ]);
    }
  }

  /**
   * Projects a SUSPENDED account with no validity end but a suspension end.
   * @param status - The account status.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function suspendedWithoutEndDateAndWithSuspensionEnd(
    status: AccountStatus
  ): AccountLifecycleUi | undefined {
    if (
      status.status === 'SUSPENDED' &&
      status.validityPeriod?.end == null &&
      toDayJs(status.suspensionPeriod?.end) != null
    ) {
      return withActions({ showSuspendedBanner: true }, [
        'deactivation.immediate',
        'deactivation.scheduled',
      ]);
    }
  }

  /**
   * Projects a SUSPENDED account more than 15 days away from deactivation.
   * @param status - The account status.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function suspendedMoreThan15DaysBeforeDeactivation(
    status: AccountStatus
  ): AccountLifecycleUi | undefined {
    if (status.status === 'SUSPENDED' && isMoreThan15Days(status)) {
      return withActions(
        {
          showSuspendedBanner: true,
          showWillDeactivateInfoText: true,
        },
        ['deactivation.immediate', 'deactivation.scheduled']
      );
    }
  }

  /**
   * Projects a SUSPENDED account 15 days or less away from deactivation.
   * @param status - The account status.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function suspendedWithin15DaysBeforeDeactivation(
    status: AccountStatus
  ): AccountLifecycleUi | undefined {
    if (status.status === 'SUSPENDED' && isWithin15Days(status)) {
      return withActions(
        {
          showSuspendedBanner: true,
          showDeactivationWarningBanner: true,
        },
        ['deactivation.immediate', 'deactivation.scheduled']
      );
    }
  }

  return computed(() => {
    const value = accountStatus.value;
    if (value == null || !value.status) {
      return null;
    }
    const now = new Date();
    return (
      futureActivation(value, now) ??
      notActivatedYet(value) ??
      deactivated(value, now) ??
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
