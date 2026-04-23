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

import {
  useFieldValidation,
  useScopedI18n,
} from '@linagora/linid-im-front-corelib';
import { useAccountCreationConfig } from 'src/composables/useAccountCreationConfig';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const requiredRule = (value) =>
  value === undefined || value === null || value === '' ? 'required' : true;
const emailRule = (value) =>
  typeof value === 'string' && value.includes('@') ? true : 'email';

vi.mock('@linagora/linid-im-front-corelib', () => ({
  useScopedI18n: vi.fn(() => ({
    t: vi.fn((key) => `translated.${key}`),
  })),
  useFieldValidation: vi.fn(() => ({
    required: requiredRule,
    email: emailRule,
  })),
}));

describe('Test composable: useAccountCreationConfig', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should scope translations and validators on the given i18n scope', () => {
    useAccountCreationConfig('AccountCreationPage');

    expect(useScopedI18n).toHaveBeenCalledWith('AccountCreationPage');
    expect(useFieldValidation).toHaveBeenCalledWith('AccountCreationPage');
  });

  it('should declare the four expected fields in order with translated labels', () => {
    const { creationFields } = useAccountCreationConfig('AccountCreationPage');

    expect(creationFields.map((field) => field.name)).toEqual([
      'externalId',
      'lastname',
      'firstname',
      'email',
    ]);
    expect(creationFields.map((field) => field.label)).toEqual([
      'translated.fields.externalId',
      'translated.fields.lastname',
      'translated.fields.firstname',
      'translated.fields.email',
    ]);
  });

  it('should apply only the required rule on non-email fields', () => {
    const { creationFields } = useAccountCreationConfig('AccountCreationPage');
    const nonEmailFields = creationFields.filter(
      (field) => field.name !== 'email'
    );

    for (const field of nonEmailFields) {
      expect(field.rules).toEqual([requiredRule]);
      expect(field.type).toBe('text');
    }
  });

  it('should apply both required and email rules on the email field', () => {
    const { creationFields } = useAccountCreationConfig('AccountCreationPage');
    const emailField = creationFields.find((field) => field.name === 'email');

    expect(emailField?.type).toBe('email');
    expect(emailField?.rules).toEqual([requiredRule, emailRule]);
  });
});
