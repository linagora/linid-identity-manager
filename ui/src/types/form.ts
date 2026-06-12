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

import type {
  LinidQBtnProps,
  LinidQDateProps,
  LinidQIconProps,
  LinidQInputProps,
  LinidQSelectProps,
} from '@linagora/linid-im-front-corelib';
import type { ValidationRule } from 'quasar';

/** Supported input types on forms. */
export type FieldType = 'text' | 'email' | 'date' | 'select';

/** Declarative definition of a single form field. */
export interface FormField<T> {
  /** Key of the field inside {@link T}. */
  name: Extract<keyof T, string>;
  /** Translated label displayed next to the input. */
  label: string;
  /** Input type rendered by the q-input component. */
  type: FieldType;
  /** Validation rules applied to the field in the order they should run. */
  rules: ValidationRule[];
  /** Static list of values offered by `select` fields. Ignored for any other field type. */
  options?: string[];
}

/**
 * UI props for the account creation form, grouped by field name. Each field carries the props for the q-input, plus the
 * icon, date picker and button components used by date fields, so a single object drives every binding.
 */
export type DatePickerUiProps = Record<
  string,
  {
    /** Props applied to the q-input component. */
    input: LinidQInputProps;
    /** Icon props for the date picker component. */
    icon: LinidQIconProps;
    /** Date props for the date picker component. */
    date: LinidQDateProps;
    /** Button props for the date picker component. */
    btn: LinidQBtnProps;
  }
>;

/**
 * UI props for the organizational unit creation form, grouped by field name. Each field carries the props for the
 * q-input and q-select components so a single object drives every field binding.
 */
export type FieldUiProps = Record<
  string,
  {
    /** Props applied to q-input fields. */
    input: LinidQInputProps;
    /** Props applied to q-select fields. */
    select: LinidQSelectProps;
  }
>;
