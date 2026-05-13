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

import type { TreeNode } from '@linagora/linid-im-front-corelib';
import type {
  OrganizationalUnitDTO,
  OrganizationalUnitForm,
  OrganizationalUnitRecord,
  OrganizationalUnitRelationDTO,
} from 'src/types/organizationalUnits';

/**
 * Composable providing utility functions to work with organizational units.
 * @returns A.
 */
export function useOrganizationalUnitMapper() {
  /**
   * Transforms an {@link OrganizationalUnitForm} into an {@link OrganizationalUnitRecord} by attaching the parent
   * identifier supplied by the navigation context.
   * @param form OU form carrying the user-editable fields.
   * @param parent UUID of the parent OU, provided by the route context.
   * @returns OU record ready to be posted to the backend.
   */
  const toOrganizationalUnitRecord = (
    form: OrganizationalUnitForm,
    parent: string
  ): OrganizationalUnitRecord => {
    return {
      parent,
      name: form.name,
      type: form.type,
    };
  };

  /**
   * Converts an organizational unit DTO into a tree node.
   * @param organizationalUnitRelation The identifier of the organizational unit to use as the node's value.
   * @param organizationalUnitDTO The organizational unit data transfer object to convert.
   * @param childrenNodes A.
   * @returns A.
   */
  function toOrganizationalUnitNode(
    organizationalUnitRelation: OrganizationalUnitRelationDTO | null,
    organizationalUnitDTO: OrganizationalUnitDTO,
    childrenNodes: TreeNode<OrganizationalUnitDTO>[]
  ): TreeNode<OrganizationalUnitDTO> {
    return {
      key: organizationalUnitRelation?.id || 'root',
      type: organizationalUnitDTO.type,
      nodes: childrenNodes,
      extraActions: [],
      value: organizationalUnitDTO,
    };
  }

  /**
   * A.
   * @param parentId A.
   * @param allOrganizationalUnit A.
   * @returns A.
   */
  function getTreeNodeChildren(
    parentId: string,
    allOrganizationalUnit: OrganizationalUnitDTO[]
  ): TreeNode<OrganizationalUnitDTO>[] {
    console.log('getTreeNodeChildren', allOrganizationalUnit);
    const result: TreeNode<OrganizationalUnitDTO>[] = [];

    for (const organizationalUnit of allOrganizationalUnit) {
      const organizationalUnitRelation = organizationalUnit.parents?.find(
        (parent) => parent.parent === parentId
      );

      if (!organizationalUnitRelation) {
        continue;
      }

      result.push(
        toOrganizationalUnitNode(
          organizationalUnitRelation,
          organizationalUnit,
          getTreeNodeChildren(organizationalUnit.id, allOrganizationalUnit)
        )
      );
    }
    return result;
  }

  /**
   * Retrieves the organizational unit tree.
   * @param allOrganizationalUnits The list of all organizational units to build the tree from.
   * @returns Promise resolving to the organizational unit tree.
   */
  function toOrganizationalUnitsTree(
    allOrganizationalUnits: OrganizationalUnitDTO[]
  ): TreeNode<OrganizationalUnitDTO>[] {
    console.log(allOrganizationalUnits);
    console.log(
      allOrganizationalUnits.filter(
        ({ parents }) => !parents?.some((p) => p.parent)
      )
    );
    return allOrganizationalUnits
      .filter(({ parents }) => !parents?.some((p) => p.parent))
      .map((root) =>
        toOrganizationalUnitNode(
          null,
          root,
          getTreeNodeChildren(root.id, allOrganizationalUnits)
        )
      );
  }

  return {
    toOrganizationalUnitRecord,
    toOrganizationalUnitsTree,
  };
}
