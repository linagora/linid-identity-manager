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

import { flushPromises, shallowMount } from '@vue/test-utils';
import { getAllOrganizationalUnit } from 'src/services/OrganizationalUnitService';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';
import OrganizationalUnitsTree from '../../../../src/components/tree/OrganizationalUnitsTree.vue';

const mockedGetAllOrganizationalUnit = vi.mocked(getAllOrganizationalUnit);

const mockNotify = vi.fn();
const mockToOrganizationalUnitsTree = vi.fn((ous) => ous);
const mockSetSelectedOrganizationalUnit = vi.fn();

const mockRouter = {
  push: vi.fn(),
  replace: vi.fn(),
};

const mockRoute = {
  query: {},
};

vi.mock('@linagora/linid-im-front-corelib', () => ({
  loadAsyncComponent: vi.fn(() => null),
  useScopedI18n: () => ({
    t: vi.fn((v) => v),
  }),
  useNotify: () => ({
    Notify: mockNotify,
  }),
}));

vi.mock('src/services/OrganizationalUnitService', () => ({
  getAllOrganizationalUnit: vi.fn(),
}));

vi.mock('src/composables/useOrganizationalUnitMapper', () => ({
  useOrganizationalUnitMapper: () => ({
    toOrganizationalUnitsTree: mockToOrganizationalUnitsTree,
  }),
}));

vi.mock('src/stores/useOrganizationalUnitStore', () => ({
  useOrganizationalUnitStore: () => ({
    setSelectedOrganizationalUnit: mockSetSelectedOrganizationalUnit,
  }),
}));

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter,
  useRoute: () => mockRoute,
}));

const rootOU = {
  id: 'root',
  name: 'Root',
  parents: [],
};

const childOU = {
  id: 'child-uuid',
  name: 'Engineering',
  parents: [{ id: '123', parent: 'root' }],
};

describe('Test component: OrganizationalUnitsTree', () => {
  let wrapper;

  beforeEach(() => {
    vi.clearAllMocks();
    mockRoute.query = {};
    mockedGetAllOrganizationalUnit.mockResolvedValue([]);
    wrapper = shallowMount(OrganizationalUnitsTree);
  });

  describe('Test function: loadData', () => {
    it('should call getAllOrganizationalUnit and map the result to tree nodes', async () => {
      const ous = [rootOU, childOU];
      const treeNodes = [{ key: 'root', label: 'Root', children: [] }];
      mockedGetAllOrganizationalUnit.mockResolvedValue(ous);
      mockToOrganizationalUnitsTree.mockReturnValue(treeNodes);

      await wrapper.vm.loadData();

      expect(getAllOrganizationalUnit).toHaveBeenCalled();
      expect(mockToOrganizationalUnitsTree).toHaveBeenCalledWith(ous);
      expect(wrapper.vm.treeNodes).toEqual(treeNodes);
    });

    it('should set selectedNode when route.query.node matches an OU by id', async () => {
      mockRoute.query = { node: 'root' };
      mockedGetAllOrganizationalUnit.mockResolvedValue([rootOU]);

      await wrapper.vm.loadData();

      expect(wrapper.vm.selectedNode).toBe('root');
    });

    it('should set selectedNode when route.query.node matches a parent relation id', async () => {
      mockRoute.query = { node: '123' };
      mockedGetAllOrganizationalUnit.mockResolvedValue([childOU]);

      await wrapper.vm.loadData();

      expect(wrapper.vm.selectedNode).toBe('123');
    });

    it('should fall back to first tree node key when route.query.node does not match any OU', async () => {
      mockRoute.query = { node: 'unknown-uuid' };
      const treeNodes = [{ key: 'root', label: 'Root' }];
      mockedGetAllOrganizationalUnit.mockResolvedValue([rootOU, childOU]);
      mockToOrganizationalUnitsTree.mockReturnValue(treeNodes);

      await wrapper.vm.loadData();

      expect(wrapper.vm.selectedNode).toBe('root');
    });

    it('should fall back to first tree node key when no route.query.node is provided', async () => {
      const treeNodes = [{ key: 'root', label: 'Root' }];
      mockedGetAllOrganizationalUnit.mockResolvedValue([rootOU]);
      mockToOrganizationalUnitsTree.mockReturnValue(treeNodes);

      await wrapper.vm.loadData();

      expect(wrapper.vm.selectedNode).toBe('root');
    });

    it('should set selectedNode to empty string when no route node and treeNodes is empty', async () => {
      mockedGetAllOrganizationalUnit.mockResolvedValue([]);
      mockToOrganizationalUnitsTree.mockReturnValue([]);

      await wrapper.vm.loadData();

      expect(wrapper.vm.selectedNode).toBe('');
    });

    it('should notify with notFound message when API returns 404', async () => {
      mockedGetAllOrganizationalUnit.mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 404 },
      });

      await wrapper.vm.loadData();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.notFound',
      });
    });

    it('should notify with generic message when a non-404 error occurs', async () => {
      mockedGetAllOrganizationalUnit.mockRejectedValueOnce(new Error('boom'));

      await wrapper.vm.loadData();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'errors.generic',
      });
    });
  });

  describe('Test watcher: selectedNode', () => {
    it('should call router.replace with the selected node id when selectedNode changes', async () => {
      mockRoute.query = { node: 'root' };
      mockedGetAllOrganizationalUnit.mockResolvedValue([rootOU]);

      await wrapper.vm.loadData();
      await nextTick();

      expect(mockRouter.replace).toHaveBeenCalledWith({
        query: { node: 'root' },
      });
    });

    it('should update store with the OU id when selectedNode matches an OU by id', async () => {
      mockRoute.query = { node: 'root' };
      mockedGetAllOrganizationalUnit.mockResolvedValue([rootOU]);

      await wrapper.vm.loadData();
      await nextTick();

      expect(mockSetSelectedOrganizationalUnit).toHaveBeenCalledWith('root');
    });

    it('should update store with the OU id when selectedNode matches a parent relation id', async () => {
      mockRoute.query = { node: '123' };
      mockedGetAllOrganizationalUnit.mockResolvedValue([childOU]);

      await wrapper.vm.loadData();
      await nextTick();

      expect(mockSetSelectedOrganizationalUnit).toHaveBeenCalledWith(
        'child-uuid'
      );
    });

    it('should update store with empty string when selectedNode does not match any OU', async () => {
      mockedGetAllOrganizationalUnit.mockResolvedValue([rootOU]);
      mockToOrganizationalUnitsTree.mockReturnValue([{ key: 'unknown-key' }]);

      await wrapper.vm.loadData();
      await nextTick();

      expect(mockSetSelectedOrganizationalUnit).toHaveBeenCalledWith('');
    });
  });

  describe('Test function: filterTreeNode', () => {
    const makeNode = (name) => ({ value: { name } });

    it('should return true when the node name contains the filter (exact match)', () => {
      expect(wrapper.vm.filterTreeNode(makeNode('Engineering'), 'Engineering')).toBe(true);
    });

    it('should return true when the node name contains the filter (partial match)', () => {
      expect(wrapper.vm.filterTreeNode(makeNode('Engineering'), 'Engin')).toBe(true);
    });

    it('should return true when the filter is case-insensitive (uppercase filter)', () => {
      expect(wrapper.vm.filterTreeNode(makeNode('Engineering'), 'ENGINEERING')).toBe(true);
    });

    it('should return true when the filter is case-insensitive (mixed case node name)', () => {
      expect(wrapper.vm.filterTreeNode(makeNode('eNgInEeRiNg'), 'engineering')).toBe(true);
    });

    it('should return true when the filter is an empty string', () => {
      expect(wrapper.vm.filterTreeNode(makeNode('Engineering'), '')).toBe(true);
    });

    it('should return false when the node name does not contain the filter', () => {
      expect(wrapper.vm.filterTreeNode(makeNode('Engineering'), 'Marketing')).toBe(false);
    });

    it('should return false when the filter is longer than the node name', () => {
      expect(wrapper.vm.filterTreeNode(makeNode('IT'), 'IT Department')).toBe(false);
    });
  });

  describe('Test hook: onMounted', () => {
    it('should call loadData on mount', async () => {
      await flushPromises();
      expect(getAllOrganizationalUnit).toHaveBeenCalledTimes(1);
    });
  });
});
