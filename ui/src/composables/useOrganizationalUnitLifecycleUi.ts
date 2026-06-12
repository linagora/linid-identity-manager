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
  OrganizationalUnitLifecycleAction,
  OrganizationalUnitLifecycleUi,
} from 'src/types/organizationalUnitLifecycleUi';
import type { OrganizationalUnitStatus } from 'src/types/organizationalUnits';
import { computed, type ComputedRef, type Ref } from 'vue';

/**
 * Composable that projects an {@link OrganizationalUnitStatus} into a deterministic UI state describing which badge,
 * banner, info text and dropdown menu items must be displayed on the OU Details page.
 *
 * The projection is the single source of truth for OU lifecycle rendering; no other component should reimplement these
 * rules. Three cases are exposed: "not suspended" (badge plus suspension dropdown), "future suspension" (badge plus
 * info text plus suspension dropdown) and "currently suspended" (badge plus suspended banner plus activation
 * dropdown).
 *
 * @param organizationalUnitStatus - Reactive reference to the OU status, or null while loading.
 * @returns A reactive UI projection, or null when the OU is not loaded.
 */
export function useOrganizationalUnitLifecycleUi(
  organizationalUnitStatus: Ref<OrganizationalUnitStatus | null>
): ComputedRef<OrganizationalUnitLifecycleUi | null> {
  const { toDayJs } = useCommonMapper();

  /**
   * Result of {@link toMenuItemsByGroup}, exposing one optional menu items list per action group. Missing properties
   * indicate empty groups.
   */
  interface MenuItemsByGroup {
    /** Menu items for the suspension dropdown. */
    suspension?: MenuItem[];
    /** Menu items for the activation dropdown. */
    activation?: MenuItem[];
  }

  /**
   * Builds the per-dropdown menu items from the given ordered list of actions. Reactivation actions are grouped under
   * activation.
   *
   * @param actions - Ordered list of dotted action keys to expose.
   * @returns Menu items split by group; missing entries when a group is empty.
   */
  function toMenuItemsByGroup(
    actions: OrganizationalUnitLifecycleAction[]
  ): MenuItemsByGroup {
    const groups = new Map<'suspension' | 'activation', MenuItem[]>();
    for (const action of actions) {
      const prefix = action.split('.')[0] as string;
      const group = prefix === 'reactivation' ? 'activation' : 'suspension';
      const items = groups.get(group) ?? [];
      items.push({ key: action, clickable: true });
      groups.set(group, items);
    }
    return {
      suspension: groups.get('suspension'),
      activation: groups.get('activation'),
    };
  }

  /**
   * Helper that builds the UI projection of a case from its base flags and its ordered action list.
   *
   * @param base - Visual flags (badge, banner, info text) for this case.
   * @param actions - Ordered list of dotted action keys.
   * @returns The full UI projection with the dropdown menu item lists.
   */
  function withActions(
    base: OrganizationalUnitLifecycleUi,
    actions: OrganizationalUnitLifecycleAction[]
  ): OrganizationalUnitLifecycleUi {
    const { suspension, activation } = toMenuItemsByGroup(actions);
    return {
      ...base,
      suspensionMenuItems: suspension,
      activationMenuItems: activation,
    };
  }

  /**
   * Projects an OU currently suspended (server-confirmed via `isSuspended`). Reactivation and suspension-end editing
   * are exposed by the suspended banner itself, so no action dropdown is rendered in this state.
   *
   * @param status - The OU status.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function currentlySuspended(
    status: OrganizationalUnitStatus
  ): OrganizationalUnitLifecycleUi | undefined {
    if (status.isSuspended) {
      return withActions({ showBadge: true, showSuspendedBanner: true }, []);
    }
  }

  /**
   * Projects an OU with a suspension scheduled strictly after `now`.
   *
   * @param status - The OU status.
   * @param now - The reference time used for time-based comparisons.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function futureSuspension(
    status: OrganizationalUnitStatus,
    now: Date
  ): OrganizationalUnitLifecycleUi | undefined {
    const suspensionStart = toDayJs(status.suspensionPeriod?.start);
    if (
      !status.isSuspended &&
      suspensionStart != null &&
      suspensionStart.isAfter(now)
    ) {
      return withActions({ showBadge: true, showWillSuspendInfoText: true }, [
        'suspension.immediate',
        'suspension.scheduled',
      ]);
    }
  }

  /**
   * Projects an OU with no active or scheduled suspension.
   *
   * @param status - The OU status.
   * @returns The UI projection, or undefined when the case does not match.
   */
  function notSuspended(
    status: OrganizationalUnitStatus
  ): OrganizationalUnitLifecycleUi | undefined {
    if (!status.isSuspended) {
      return withActions({ showBadge: true }, [
        'suspension.immediate',
        'suspension.scheduled',
      ]);
    }
  }

  return computed(() => {
    const value = organizationalUnitStatus.value;
    if (value == null) {
      return null;
    }
    const now = new Date();
    return (
      currentlySuspended(value) ??
      futureSuspension(value, now) ??
      notSuspended(value) ??
      {}
    );
  });
}
