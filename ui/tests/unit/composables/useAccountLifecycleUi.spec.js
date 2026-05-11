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

import { useAccountLifecycleUi } from 'src/composables/useAccountLifecycleUi';
import { ACCOUNT_LIFECYCLE_ACTIONS } from 'src/types/accountLifecycleUi';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: vi.fn((v) => v) }),
}));

const NOW_ISO = '2026-05-04T12:00:00Z';

beforeEach(() => vi.useFakeTimers().setSystemTime(new Date(NOW_ISO)));
afterEach(() => vi.useRealTimers());

const buildStatus = (overrides = {}) => ({
  status: 'ACTIVE',
  validityPeriod: { start: '2026-01-01T00:00:00Z', end: null },
  suspensionPeriod: { start: null, end: null },
  activationAt: '2026-01-02T00:00:00Z',
  statusReason: null,
  statusSubreason: null,
  statusComment: null,
  daysBeforeDeactivation: null,
  ...overrides,
});

const project = (status) => {
  const status$ = ref(status);
  return useAccountLifecycleUi(status$).value;
};

const expectMenuItems = (ui, { activation, suspension, deactivation } = {}) => {
  expect(ui?.activationMenuItems).toEqual(activation);
  expect(ui?.suspensionMenuItems).toEqual(suspension);
  expect(ui?.deactivationMenuItems).toEqual(deactivation);
};

const item = (key) => ({ key, clickable: true });

describe('Test composable: useAccountLifecycleUi', () => {
  it('exposes ACCOUNT_LIFECYCLE_ACTIONS as a frozen tuple', () => {
    expect(ACCOUNT_LIFECYCLE_ACTIONS).toEqual([
      'activation.immediate',
      'activation.scheduled',
      'suspension.immediate',
      'suspension.scheduled',
      'suspension.modify',
      'deactivation.immediate',
      'deactivation.scheduled',
      'deactivation.modify',
      'reactivation.immediate',
    ]);
  });

  it('returns null when the status ref holds null', () => {
    const status$ = ref(null);
    expect(useAccountLifecycleUi(status$).value).toBeNull();
  });

  it('returns null when the status ref holds undefined', () => {
    const status$ = ref(undefined);
    expect(useAccountLifecycleUi(status$).value).toBeNull();
  });

  it('returns null when status field is missing', () => {
    const status$ = ref(buildStatus({ status: undefined }));
    expect(useAccountLifecycleUi(status$).value).toBeNull();
  });

  it('reacts to account status changes', () => {
    const status$ = ref(
      buildStatus({ status: 'INACTIVE', activationAt: null })
    );
    const ui = useAccountLifecycleUi(status$);
    expect(ui.value?.showBadge).toBe(true);
    expect(ui.value?.showNotActivatedInfoText).toBe(true);

    status$.value = buildStatus({ status: 'ACTIVE' });
    expect(ui.value?.showBadge).toBe(true);
    expect(ui.value?.showNotActivatedInfoText).toBeFalsy();
  });

  it('falls back to an empty projection when no branch matches', () => {
    // ACTIVE with non-null end date string but no daysBeforeDeactivation:
    // none of the active* branches qualify, so the projection is empty.
    const ui = project(
      buildStatus({
        status: 'ACTIVE',
        validityPeriod: {
          start: '2026-01-01T00:00:00Z',
          end: '2026-12-01T00:00:00Z',
        },
        suspensionPeriod: { start: null, end: null },
        daysBeforeDeactivation: null,
      })
    );
    expect(ui).toEqual({});
  });
});

describe('Test case: INACTIVE — future activation', () => {
  it('matches when validity.start > now', () => {
    const ui = project(
      buildStatus({
        status: 'INACTIVE',
        validityPeriod: { start: '2026-06-01T00:00:00Z', end: null },
        activationAt: null,
      })
    );
    expect(ui?.showBadge).toBe(true);
    expect(ui?.showNotActivatedInfoText).toBeFalsy();
    expectMenuItems(ui, {
      activation: [item('activation.immediate'), item('activation.scheduled')],
      suspension: [item('suspension.scheduled')],
      deactivation: [item('deactivation.scheduled')],
    });
  });
});

describe('Test case: INACTIVE — not activated yet', () => {
  it('matches when status is INACTIVE and activationAt is null', () => {
    const ui = project(
      buildStatus({
        status: 'INACTIVE',
        validityPeriod: { start: '2026-01-01T00:00:00Z', end: null },
        activationAt: null,
      })
    );
    expect(ui?.showBadge).toBe(true);
    expect(ui?.showNotActivatedInfoText).toBe(true);
    expectMenuItems(ui, {
      suspension: [item('suspension.scheduled')],
      deactivation: [item('deactivation.scheduled')],
    });
  });

  it('does not match when activationAt is set', () => {
    const ui = project(
      buildStatus({
        status: 'INACTIVE',
        validityPeriod: { start: '2026-01-01T00:00:00Z', end: null },
        activationAt: '2026-01-02T00:00:00Z',
      })
    );
    expect(ui?.showNotActivatedInfoText).toBeFalsy();
  });
});

describe('Test case: ACTIVE — no end date, no future suspension', () => {
  it('matches and exposes the full action set', () => {
    const ui = project(
      buildStatus({
        status: 'ACTIVE',
        validityPeriod: { start: '2026-01-01T00:00:00Z', end: null },
        suspensionPeriod: { start: null, end: null },
        daysBeforeDeactivation: null,
      })
    );
    expect(ui?.showBadge).toBe(true);
    expect(ui?.showWillDeactivateInfoText).toBeFalsy();
    expect(ui?.showWillSuspendInfoText).toBeFalsy();
    expectMenuItems(ui, {
      suspension: [item('suspension.immediate'), item('suspension.scheduled')],
      deactivation: [
        item('deactivation.immediate'),
        item('deactivation.scheduled'),
      ],
    });
  });

  it('ignores past suspension start when none is in the future', () => {
    const ui = project(
      buildStatus({
        status: 'ACTIVE',
        validityPeriod: { start: '2026-01-01T00:00:00Z', end: null },
        suspensionPeriod: {
          start: '2026-01-01T00:00:00Z',
          end: '2026-02-01T00:00:00Z',
        },
      })
    );
    expect(ui?.showBadge).toBe(true);
    expect(ui?.showWillSuspendInfoText).toBeFalsy();
  });
});

describe('Test case: ACTIVE — end date > 15 days, no future suspension', () => {
  it('matches and shows the will-deactivate info text', () => {
    const ui = project(
      buildStatus({
        status: 'ACTIVE',
        validityPeriod: {
          start: '2026-01-01T00:00:00Z',
          end: '2026-12-01T00:00:00Z',
        },
        daysBeforeDeactivation: 30,
      })
    );
    expect(ui?.showBadge).toBe(true);
    expect(ui?.showWillDeactivateInfoText).toBe(true);
    expect(ui?.showDeactivationWarningBanner).toBeFalsy();
    expectMenuItems(ui, {
      suspension: [item('suspension.immediate'), item('suspension.scheduled')],
      deactivation: [
        item('deactivation.immediate'),
        item('deactivation.modify'),
      ],
    });
  });

  it('treats daysBeforeDeactivation = 16 as more-than-15 (boundary)', () => {
    const ui = project(
      buildStatus({
        status: 'ACTIVE',
        validityPeriod: {
          start: '2026-01-01T00:00:00Z',
          end: '2026-05-20T00:00:00Z',
        },
        daysBeforeDeactivation: 16,
      })
    );
    expect(ui?.showWillDeactivateInfoText).toBe(true);
  });
});

describe('ACTIVE — end date <= 15 days, no future suspension', () => {
  it('matches and shows the deactivation warning banner', () => {
    const ui = project(
      buildStatus({
        status: 'ACTIVE',
        validityPeriod: {
          start: '2026-01-01T00:00:00Z',
          end: '2026-05-15T00:00:00Z',
        },
        daysBeforeDeactivation: 11,
      })
    );
    expect(ui?.showBadge).toBe(true);
    expect(ui?.showDeactivationWarningBanner).toBe(true);
    expect(ui?.showWillDeactivateInfoText).toBeFalsy();
    expectMenuItems(ui, {
      suspension: [item('suspension.immediate'), item('suspension.scheduled')],
    });
  });

  it('treats daysBeforeDeactivation = 15 as within-15 (boundary)', () => {
    const ui = project(
      buildStatus({
        status: 'ACTIVE',
        validityPeriod: {
          start: '2026-01-01T00:00:00Z',
          end: '2026-05-19T00:00:00Z',
        },
        daysBeforeDeactivation: 15,
      })
    );
    expect(ui?.showDeactivationWarningBanner).toBe(true);
  });
});

describe('Test case: ACTIVE — no end date, suspension planned', () => {
  it('matches and shows the will-suspend info text', () => {
    const ui = project(
      buildStatus({
        status: 'ACTIVE',
        validityPeriod: { start: '2026-01-01T00:00:00Z', end: null },
        suspensionPeriod: { start: '2026-06-01T00:00:00Z', end: null },
        daysBeforeDeactivation: null,
      })
    );
    expect(ui?.showBadge).toBe(true);
    expect(ui?.showWillSuspendInfoText).toBe(true);
    expect(ui?.showWillDeactivateInfoText).toBeFalsy();
    expectMenuItems(ui, {
      suspension: [item('suspension.immediate'), item('suspension.scheduled')],
      deactivation: [
        item('deactivation.immediate'),
        item('deactivation.scheduled'),
      ],
    });
  });
});

describe('Test case: ACTIVE — end date > 15 days + future suspension', () => {
  it('shows both info texts', () => {
    const ui = project(
      buildStatus({
        status: 'ACTIVE',
        validityPeriod: {
          start: '2026-01-01T00:00:00Z',
          end: '2026-12-01T00:00:00Z',
        },
        suspensionPeriod: { start: '2026-06-01T00:00:00Z', end: null },
        daysBeforeDeactivation: 30,
      })
    );
    expect(ui?.showWillDeactivateInfoText).toBe(true);
    expect(ui?.showWillSuspendInfoText).toBe(true);
    expectMenuItems(ui, {
      suspension: [item('suspension.immediate'), item('suspension.scheduled')],
      deactivation: [
        item('deactivation.immediate'),
        item('deactivation.modify'),
      ],
    });
  });
});

describe('Test case: ACTIVE — end date <= 15 days + future suspension', () => {
  it('shows the warning banner and the will-suspend info text', () => {
    const ui = project(
      buildStatus({
        status: 'ACTIVE',
        validityPeriod: {
          start: '2026-01-01T00:00:00Z',
          end: '2026-05-12T00:00:00Z',
        },
        suspensionPeriod: { start: '2026-05-10T00:00:00Z', end: null },
        daysBeforeDeactivation: 8,
      })
    );
    expect(ui?.showDeactivationWarningBanner).toBe(true);
    expect(ui?.showWillSuspendInfoText).toBe(true);
    expect(ui?.showWillDeactivateInfoText).toBeFalsy();
    expectMenuItems(ui, {
      suspension: [item('suspension.immediate'), item('suspension.scheduled')],
    });
  });
});

describe('Test case: SUSPENDED — no validity end, no suspension end', () => {
  it('shows only the suspended banner', () => {
    const ui = project(
      buildStatus({
        status: 'SUSPENDED',
        validityPeriod: { start: '2026-01-01T00:00:00Z', end: null },
        suspensionPeriod: { start: '2026-04-01T00:00:00Z', end: null },
        daysBeforeDeactivation: null,
      })
    );
    expect(ui?.showBadge).toBeFalsy();
    expect(ui?.showSuspendedBanner).toBe(true);
    expect(ui?.showDeactivationWarningBanner).toBeFalsy();
    expectMenuItems(ui, {
      deactivation: [
        item('deactivation.immediate'),
        item('deactivation.scheduled'),
      ],
    });
  });
});

describe('Test case: SUSPENDED — no validity end, with suspension end', () => {
  it('shows only the suspended banner', () => {
    const ui = project(
      buildStatus({
        status: 'SUSPENDED',
        validityPeriod: { start: '2026-01-01T00:00:00Z', end: null },
        suspensionPeriod: {
          start: '2026-04-01T00:00:00Z',
          end: '2026-08-01T00:00:00Z',
        },
        daysBeforeDeactivation: null,
      })
    );
    expect(ui?.showSuspendedBanner).toBe(true);
    expect(ui?.showDeactivationWarningBanner).toBeFalsy();
  });
});

describe('Test case: SUSPENDED — end date > 15 days', () => {
  it('shows the suspended banner and will-deactivate info text', () => {
    const ui = project(
      buildStatus({
        status: 'SUSPENDED',
        validityPeriod: {
          start: '2026-01-01T00:00:00Z',
          end: '2026-12-01T00:00:00Z',
        },
        suspensionPeriod: { start: '2026-04-01T00:00:00Z', end: null },
        daysBeforeDeactivation: 30,
      })
    );
    expect(ui?.showBadge).toBeFalsy();
    expect(ui?.showSuspendedBanner).toBe(true);
    expect(ui?.showWillDeactivateInfoText).toBe(true);
    expect(ui?.showDeactivationWarningBanner).toBeFalsy();
  });
});

describe('Test case: SUSPENDED — end date <= 15 days', () => {
  it('shows the suspended banner and the deactivation warning banner', () => {
    const ui = project(
      buildStatus({
        status: 'SUSPENDED',
        validityPeriod: {
          start: '2026-01-01T00:00:00Z',
          end: '2026-05-12T00:00:00Z',
        },
        suspensionPeriod: { start: '2026-04-01T00:00:00Z', end: null },
        daysBeforeDeactivation: 8,
      })
    );
    expect(ui?.showSuspendedBanner).toBe(true);
    expect(ui?.showDeactivationWarningBanner).toBe(true);
    expect(ui?.showWillDeactivateInfoText).toBeFalsy();
  });
});
