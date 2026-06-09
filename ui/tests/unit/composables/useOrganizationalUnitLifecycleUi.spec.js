/*
 * Copyright (C) 2026 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
 * which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
 * of the interface window, the display of the "You are using the Open Source and free version of LinID™, powered by
 * Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!" infobox and in the e-mails
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

import { useOrganizationalUnitLifecycleUi } from 'src/composables/useOrganizationalUnitLifecycleUi';
import { ORGANIZATIONAL_UNIT_LIFECYCLE_ACTIONS } from 'src/types/organizationalUnitLifecycleUi';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: vi.fn((v) => v) }),
}));

const NOW_ISO = '2026-05-04T12:00:00Z';

beforeEach(() => vi.useFakeTimers().setSystemTime(new Date(NOW_ISO)));
afterEach(() => vi.useRealTimers());

const buildStatus = (overrides = {}) => ({
  suspensionPeriod: null,
  statusReason: null,
  statusSubreason: null,
  statusComment: null,
  isSuspended: false,
  ...overrides,
});

const project = (status) => {
  const status$ = ref(status);
  return useOrganizationalUnitLifecycleUi(status$).value;
};

const item = (key) => ({ key, clickable: true });

describe('Test composable: useOrganizationalUnitLifecycleUi', () => {
  it('exposes ORGANIZATIONAL_UNIT_LIFECYCLE_ACTIONS as a tuple', () => {
    expect(ORGANIZATIONAL_UNIT_LIFECYCLE_ACTIONS).toEqual([
      'suspension.immediate',
      'suspension.scheduled',
      'suspension.modify-end',
      'reactivation.immediate',
    ]);
  });

  it('returns null when the status reference is null', () => {
    expect(project(null)).toBe(null);
  });

  describe('Case: not suspended', () => {
    it('shows the badge and the suspension dropdown with immediate + scheduled', () => {
      const ui = project(buildStatus({ isSuspended: false }));

      expect(ui).toEqual({
        showBadge: true,
        suspensionMenuItems: [
          item('suspension.immediate'),
          item('suspension.scheduled'),
        ],
        activationMenuItems: undefined,
      });
    });
  });

  describe('Case: future suspension', () => {
    it('shows the badge, the will-suspend info text and the suspension dropdown', () => {
      const ui = project(
        buildStatus({
          isSuspended: false,
          suspensionPeriod: { start: '2026-06-01T00:00:00Z', end: null },
        })
      );

      expect(ui).toEqual({
        showBadge: true,
        showWillSuspendInfoText: true,
        suspensionMenuItems: [
          item('suspension.immediate'),
          item('suspension.scheduled'),
        ],
        activationMenuItems: undefined,
      });
    });

    it('falls back to "not suspended" when the suspension start equals now', () => {
      const ui = project(
        buildStatus({
          isSuspended: false,
          suspensionPeriod: { start: NOW_ISO, end: null },
        })
      );

      expect(ui.showWillSuspendInfoText).toBeFalsy();
      expect(ui.suspensionMenuItems).toEqual([
        item('suspension.immediate'),
        item('suspension.scheduled'),
      ]);
    });
  });

  describe('Case: currently suspended', () => {
    it('shows the badge and the suspended banner without any action dropdown', () => {
      const ui = project(
        buildStatus({
          isSuspended: true,
          suspensionPeriod: { start: '2026-05-01T00:00:00Z', end: null },
        })
      );

      expect(ui).toEqual({
        showBadge: true,
        showSuspendedBanner: true,
        suspensionMenuItems: undefined,
        activationMenuItems: undefined,
      });
    });

    it('takes precedence over a future-suspension start in the past', () => {
      const ui = project(
        buildStatus({
          isSuspended: true,
          suspensionPeriod: {
            start: '2026-04-01T00:00:00Z',
            end: '2026-12-31T00:00:00Z',
          },
        })
      );

      expect(ui.showSuspendedBanner).toBe(true);
      expect(ui.showWillSuspendInfoText).toBeFalsy();
    });
  });
});
