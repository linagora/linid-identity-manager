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

import { shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OrganizationalUnitLifecyclePanel from '../../../../src/components/panel/OrganizationalUnitLifecyclePanel.vue';
import { mockToDayJs } from '../../helpers/mockCommonMapper.js';

const { mockUiEventSubjectNext } = vi.hoisted(() => ({
  mockUiEventSubjectNext: vi.fn(),
}));

vi.mock('@linagora/lin-pulsar-core', () => ({
  loadAsyncComponent: vi.fn(() => null),
  uiEventSubject: { next: mockUiEventSubjectNext },
  useCommonMapper: () => ({
    toDayJs: mockToDayJs(),
  }),
}));

const activeOrganizationalUnitDto = {
  id: 'test-ou-id',
  name: 'Company A',
  type: 'COMPANY',
  createdBy: 'Alice Creator',
  updatedBy: 'Bob Updater',
  insertDate: '2026-04-15T12:00:24.814930Z',
  updateDate: '2026-04-16T09:30:00.000000Z',
  suspensionPeriod: null,
  isSuspended: false,
  extraParameters: {},
};

const suspendedOrganizationalUnitDto = {
  ...activeOrganizationalUnitDto,
  suspensionPeriod: { start: '2026-05-01T00:00:00Z', end: null },
  isSuspended: true,
};

/**
 * Mounts the panel with the given entity, mimicking the zone renderer of the generic details page.
 *
 * @param entity - The raw organizational unit entity provided by the hosting page.
 * @returns The shallow-mounted wrapper.
 */
function mountPanel(entity = activeOrganizationalUnitDto) {
  return shallowMount(OrganizationalUnitLifecyclePanel, {
    props: {
      entity,
      uiNamespace: 'moduleOrganizationalUnitDetailsPage',
    },
  });
}

describe('Test component: OrganizationalUnitLifecyclePanel', () => {
  let wrapper;

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Test computed: organizationalUnitStatus', () => {
    it('should be null while the entity is not resolved', () => {
      wrapper = mountPanel({});

      expect(wrapper.vm.organizationalUnitStatus).toBeNull();
    });

    it('should expose only the lifecycle fields of the resolved entity', () => {
      wrapper = mountPanel(suspendedOrganizationalUnitDto);

      expect(wrapper.vm.organizationalUnitStatus).toEqual({
        suspensionPeriod: { start: '2026-05-01T00:00:00Z', end: null },
        isSuspended: true,
      });
    });
  });

  describe('Test computed: lifecycleUi', () => {
    it('should be null while the entity is not resolved', () => {
      wrapper = mountPanel({});

      expect(wrapper.vm.lifecycleUi).toBeNull();
    });

    it('should expose the suspension actions for a non-suspended entity', () => {
      wrapper = mountPanel();

      const ui = wrapper.vm.lifecycleUi;
      expect(ui.showBadge).toBe(true);
      expect(ui.showSuspendedBanner).toBeUndefined();
      expect(ui.suspensionMenuItems).toEqual([
        { key: 'suspension.immediate', clickable: true },
        { key: 'suspension.scheduled', clickable: true },
      ]);
      expect(ui.activationMenuItems).toBeUndefined();
    });

    it('should expose the suspended banner without dropdown actions for a suspended entity', () => {
      wrapper = mountPanel(suspendedOrganizationalUnitDto);

      const ui = wrapper.vm.lifecycleUi;
      expect(ui.showSuspendedBanner).toBe(true);
      expect(ui.suspensionMenuItems).toBeUndefined();
      expect(ui.activationMenuItems).toBeUndefined();
    });
  });

  describe('Test computed: lifecycleItems', () => {
    it('should be empty while the entity is not resolved', () => {
      wrapper = mountPanel({});

      expect(wrapper.vm.lifecycleItems).toEqual({
        activation: undefined,
        suspension: undefined,
      });
    });

    it('should index the menu items of the lifecycle UI by button type', () => {
      wrapper = mountPanel();

      expect(wrapper.vm.lifecycleItems).toEqual({
        activation: undefined,
        suspension: [
          { key: 'suspension.immediate', clickable: true },
          { key: 'suspension.scheduled', clickable: true },
        ],
      });
    });
  });

  describe('Test function: runCardAction', () => {
    it('should ask the lifecycle buttons card to run the action', () => {
      wrapper = mountPanel(suspendedOrganizationalUnitDto);

      wrapper.vm.runCardAction('suspension.modify-end');

      expect(mockUiEventSubjectNext).toHaveBeenCalledWith({
        key: 'organizational-unit-lifecycle-action',
        data: { key: 'suspension.modify-end' },
      });
    });
  });
});
