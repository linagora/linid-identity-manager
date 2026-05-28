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

import { shallowMount } from '@vue/test-utils';
import OrganizationalUnitSuspendedBanner from 'components/banner/OrganizationalUnitSuspendedBanner.vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useI18n } from 'vue-i18n';

const tMock = vi.fn((key) => key);
const uiMock = vi.fn(() => ({}));
const toDateMock = vi.fn((value) => (value ? `formatted(${value})` : '-'));

vi.mock('@linagora/linid-im-front-corelib', () => ({
  useScopedI18n: () => ({ t: tMock }),
  useUiDesign: () => ({ ui: uiMock }),
}));

vi.mock('src/composables/useCommonMapper', () => ({
  useCommonMapper: () => ({ toDate: toDateMock }),
}));

vi.mock('vue-i18n', () => ({
  useI18n: vi.fn(),
}));

const buildStatus = (overrides = {}) => ({
  isSuspended: true,
  suspensionPeriod: { start: '2026-05-01T00:00:00Z', end: null },
  statusReason: null,
  statusSubreason: null,
  statusComment: null,
  ...overrides,
});

describe('Test component: OrganizationalUnitSuspendedBanner', () => {
  beforeEach(() => {
    useI18n.mockReturnValue({ t: tMock });
  });

  describe('Test computed: contentI18nKey', () => {
    it('should select the open-ended content when end is null', () => {
      const wrapper = shallowMount(OrganizationalUnitSuspendedBanner, {
        props: { organizationalUnitStatus: buildStatus() },
      });
      expect(wrapper.vm.contentI18nKey).toBe('content');
    });

    it('should select the bounded content when end is set', () => {
      const wrapper = shallowMount(OrganizationalUnitSuspendedBanner, {
        props: {
          organizationalUnitStatus: buildStatus({
            suspensionPeriod: {
              start: '2026-05-01T00:00:00Z',
              end: '2026-12-31T00:00:00Z',
            },
          }),
        },
      });
      expect(wrapper.vm.contentI18nKey).toBe('contentWithEndDate');
    });
  });

  describe('Test computed: startDate and endDate', () => {
    it('should format the suspension start through the common mapper', () => {
      const wrapper = shallowMount(OrganizationalUnitSuspendedBanner, {
        props: { organizationalUnitStatus: buildStatus() },
      });
      expect(wrapper.vm.startDate).toBe('formatted(2026-05-01T00:00:00Z)');
      expect(toDateMock).toHaveBeenCalledWith('2026-05-01T00:00:00Z');
    });

    it('should format an absent end as "-"', () => {
      const wrapper = shallowMount(OrganizationalUnitSuspendedBanner, {
        props: { organizationalUnitStatus: buildStatus() },
      });
      expect(wrapper.vm.endDate).toBe('-');
    });
  });
});
