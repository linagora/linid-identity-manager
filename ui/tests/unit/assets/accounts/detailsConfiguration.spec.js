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

import { fieldsOrder } from 'src/assets/accounts/detailsConfiguration';
import { describe, expect, it } from 'vitest';

/**
 * Full list of keys on the Account interface.
 * Keep this in sync with src/types/accounts.ts — that is the whole point of this test.
 */
const ALL_ACCOUNT_KEYS = [
  'id',
  'lastname',
  'firstname',
  'email',
  'externalId',
  'createdBy',
  'updatedBy',
  'insertDate',
  'updateDate',
];

/**
 * Keys intentionally omitted from the details view.
 * Justify each exclusion with a comment so the decision is auditable.
 */
const NON_DISPLAYABLE_KEYS = new Set([
  'id', // internal identifier, never shown to the user
  'externalId', // business identifier managed externally, not surfaced in the details card
]);

const EXPECTED_FIELDS = ALL_ACCOUNT_KEYS.filter(
  (key) => !NON_DISPLAYABLE_KEYS.has(key)
);

describe('Test assets: detailsConfiguration', () => {
  describe('Test constant: fieldsOrder', () => {
    it('should cover every displayable Account key', () => {
      expect(fieldsOrder).toEqual(expect.arrayContaining(EXPECTED_FIELDS));
    });

    it('should not contain unknown or non-displayable Account keys', () => {
      for (const field of fieldsOrder) {
        expect(ALL_ACCOUNT_KEYS).toContain(field);
        expect(NON_DISPLAYABLE_KEYS.has(field)).toBe(false);
      }
    });

    it('should have the same length as the set of displayable keys', () => {
      expect(fieldsOrder).toHaveLength(EXPECTED_FIELDS.length);
    });
  });
});
