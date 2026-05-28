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

import { useOrganizationalUnitMapper } from 'src/composables/useOrganizationalUnitMapper';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useI18n } from 'vue-i18n';

const PARENT_UUID = '00000000-0000-4000-8000-000000000000';
const OU_UUID = '11111111-1111-4111-8111-111111111111';
const CREATOR_UUID = '22222222-2222-4222-8222-222222222222';

vi.mock('vue-i18n', () => ({
  useI18n: vi.fn(),
}));

const buildDto = (overrides = {}) => ({
  id: OU_UUID,
  name: 'Engineering',
  type: 'DEPARTMENT',
  createdBy: CREATOR_UUID,
  updatedBy: CREATOR_UUID,
  insertDate: '2026-05-13T12:00:00Z',
  updateDate: '2026-05-13T12:00:00Z',
  suspensionPeriod: null,
  statusReason: null,
  statusSubreason: null,
  statusComment: null,
  isSuspended: false,
  parents: [],
  ...overrides,
});

describe('Test mapper: organizationalUnitMapper', () => {
  const tMock = vi.fn();

  beforeEach(() => {
    tMock.mockReset();
    tMock.mockReturnValue('YYYY/MM/DD');
    useI18n.mockReturnValue({ t: tMock });
  });

  describe('Test function: toOrganizationalUnitRecord', () => {
    it('should attach the parent identifier to the form values', () => {
      const { toOrganizationalUnitRecord } = useOrganizationalUnitMapper();

      const form = { name: 'Engineering', type: 'DEPARTMENT' };

      expect(toOrganizationalUnitRecord(form, PARENT_UUID)).toEqual({
        parent: PARENT_UUID,
        name: 'Engineering',
        type: 'DEPARTMENT',
      });
    });

    it('should not mutate the source form', () => {
      const { toOrganizationalUnitRecord } = useOrganizationalUnitMapper();

      const form = { name: 'Engineering', type: 'DEPARTMENT' };
      toOrganizationalUnitRecord(form, PARENT_UUID);

      expect(form).toEqual({ name: 'Engineering', type: 'DEPARTMENT' });
    });
  });

  describe('Test function: toOrganizationalUnitsTree', () => {
    const rootOU = {
      id: 'root-id',
      name: 'Root',
      type: 'root',
      parents: [],
    };

    const childOU = {
      id: 'child-id',
      name: 'Engineering',
      type: 'DEPARTMENT',
      parents: [{ id: 'rel-1', parent: 'root-id' }],
    };

    const grandChildOU = {
      id: 'grand-id',
      name: 'Backend',
      type: 'TEAM',
      parents: [{ id: 'rel-2', parent: 'child-id' }],
    };

    const secondRootOU = {
      id: 'root2-id',
      name: 'Second Root',
      type: 'root',
      parents: [],
    };

    it('should return an empty array when no OUs are provided', () => {
      const { toOrganizationalUnitsTree } = useOrganizationalUnitMapper();
      expect(toOrganizationalUnitsTree([])).toEqual([]);
    });

    it('should return a single root node with no children', () => {
      const { toOrganizationalUnitsTree } = useOrganizationalUnitMapper();

      const result = toOrganizationalUnitsTree([rootOU]);

      expect(result).toEqual([
        {
          key: 'root-id',
          type: 'root',
          nodes: [],
          extraActions: [],
          value: rootOU,
        },
      ]);
    });

    it('should use the OU id as key for root nodes', () => {
      const { toOrganizationalUnitsTree } = useOrganizationalUnitMapper();

      const result = toOrganizationalUnitsTree([rootOU]);

      expect(result[0].key).toBe('root-id');
    });

    it('should use the relation id as key for child nodes', () => {
      const { toOrganizationalUnitsTree } = useOrganizationalUnitMapper();

      const result = toOrganizationalUnitsTree([rootOU, childOU]);

      expect(result[0].nodes[0].key).toBe('rel-1');
    });

    it('should attach child nodes under the correct parent', () => {
      const { toOrganizationalUnitsTree } = useOrganizationalUnitMapper();

      const result = toOrganizationalUnitsTree([rootOU, childOU]);

      expect(result).toHaveLength(1);
      expect(result[0].nodes).toHaveLength(1);
      expect(result[0].nodes[0].value).toEqual(childOU);
    });

    it('should build a multi-level tree recursively', () => {
      const { toOrganizationalUnitsTree } = useOrganizationalUnitMapper();

      const result = toOrganizationalUnitsTree([rootOU, childOU, grandChildOU]);

      const grandChildNode = result[0].nodes[0].nodes[0];
      expect(grandChildNode.key).toBe('rel-2');
      expect(grandChildNode.value).toEqual(grandChildOU);
      expect(grandChildNode.nodes).toEqual([]);
    });

    it('should return multiple root nodes when several OUs have no parent', () => {
      const { toOrganizationalUnitsTree } = useOrganizationalUnitMapper();

      const result = toOrganizationalUnitsTree([rootOU, secondRootOU]);

      expect(result).toHaveLength(2);
      expect(result[0].key).toBe('root-id');
      expect(result[1].key).toBe('root2-id');
    });

    it('should attach multiple children under the same parent', () => {
      const { toOrganizationalUnitsTree } = useOrganizationalUnitMapper();

      const childOU2 = {
        id: 'child2-id',
        name: 'Marketing',
        type: 'DEPARTMENT',
        parents: [{ id: 'rel-3', parent: 'root-id' }],
      };

      const result = toOrganizationalUnitsTree([rootOU, childOU, childOU2]);

      expect(result[0].nodes).toHaveLength(2);
      expect(result[0].nodes[0].key).toBe('rel-1');
      expect(result[0].nodes[1].key).toBe('rel-3');
    });

    it('should set the correct type and empty extraActions on a child node', () => {
      const { toOrganizationalUnitsTree } = useOrganizationalUnitMapper();

      const result = toOrganizationalUnitsTree([rootOU, childOU]);

      const childNode = result[0].nodes[0];
      expect(childNode.type).toBe('DEPARTMENT');
      expect(childNode.extraActions).toEqual([]);
    });

    it('should set the correct type and empty extraActions on a root node', () => {
      const { toOrganizationalUnitsTree } = useOrganizationalUnitMapper();

      const result = toOrganizationalUnitsTree([rootOU]);

      expect(result[0].type).toBe('root');
      expect(result[0].extraActions).toEqual([]);
    });

    it('should not include a node as root if it has at least one parent relation', () => {
      const { toOrganizationalUnitsTree } = useOrganizationalUnitMapper();

      const result = toOrganizationalUnitsTree([rootOU, childOU]);

      expect(result).toHaveLength(1);
      expect(result[0].key).toBe('root-id');
    });
  });

  describe('Test function: toOrganizationalUnit', () => {
    it('should project identity fields from the DTO', () => {
      const { toOrganizationalUnit } = useOrganizationalUnitMapper();

      const dto = buildDto();
      const result = toOrganizationalUnit(dto);

      expect(result).toEqual({
        id: OU_UUID,
        name: 'Engineering',
        type: 'DEPARTMENT',
        createdBy: CREATOR_UUID,
        updatedBy: CREATOR_UUID,
        insertDate: '2026/05/13',
        updateDate: '2026/05/13',
      });
    });

    it('should omit suspension fields from the identity projection', () => {
      const { toOrganizationalUnit } = useOrganizationalUnitMapper();

      const dto = buildDto({
        isSuspended: true,
        suspensionPeriod: { start: '2026-06-01T00:00:00Z', end: null },
        statusReason: 'AUDIT',
      });
      const result = toOrganizationalUnit(dto);

      expect(result).not.toHaveProperty('isSuspended');
      expect(result).not.toHaveProperty('suspensionPeriod');
      expect(result).not.toHaveProperty('statusReason');
    });
  });

  describe('Test function: toOrganizationalUnitStatus', () => {
    it('should project suspension fields from the DTO', () => {
      const { toOrganizationalUnitStatus } = useOrganizationalUnitMapper();

      const dto = buildDto({
        isSuspended: true,
        suspensionPeriod: { start: '2026-06-01T00:00:00Z', end: null },
        statusReason: 'AUDIT',
        statusSubreason: 'INTERNAL_REVIEW',
        statusComment: 'Pending verification',
      });
      const result = toOrganizationalUnitStatus(dto);

      expect(result).toEqual({
        suspensionPeriod: { start: '2026-06-01T00:00:00Z', end: null },
        statusReason: 'AUDIT',
        statusSubreason: 'INTERNAL_REVIEW',
        statusComment: 'Pending verification',
        isSuspended: true,
      });
    });

    it('should omit identity fields from the status projection', () => {
      const { toOrganizationalUnitStatus } = useOrganizationalUnitMapper();

      const dto = buildDto();
      const result = toOrganizationalUnitStatus(dto);

      expect(result).not.toHaveProperty('id');
      expect(result).not.toHaveProperty('name');
      expect(result).not.toHaveProperty('type');
    });
  });
});
