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

import type { UiEvent } from '@linagora/linid-im-front-corelib';
import {
  getI18nInstance,
  uiEventSubject,
} from '@linagora/linid-im-front-corelib';
import type { LifecycleFormField } from 'src/assets/accounts/accountLifecycleUiConfiguration';
import type { Composer } from 'vue-i18n';

/**
 * Options shared by both lifecycle dialog kinds.
 */
interface BaseDialogOptions {
  /**
   * The i18n scope of the dialog (title / content / fields / buttons), e.g.
   * `AccountSuspensionActions.FormDialog.immediate`.
   */
  i18nScope: string;
  /**
   * Optional per-dialog UI design namespace, overriding the composable default.
   */
  uiNamespace?: string;
}

/**
 * Options for a form dialog: the user fills `formFields` before confirming.
 * @template TForm - The flat form payload type emitted on submit.
 */
interface FormDialogOptions<TForm> extends BaseDialogOptions {
  /**
   * Form fields rendered in the dialog.
   */
  formFields: readonly LifecycleFormField[];
  /**
   * Optional initial values used to pre-fill the form fields, for instance when
   * editing an existing suspension. Omitted for dialogs that must open empty.
   */
  initialFormData?: TForm;
  /**
   * Handler invoked with the submitted form data. The caller owns the full
   * post-submit workflow (update call, notifications, error handling, …).
   */
  onSubmit: (formData: TForm) => void;
}

/**
 * Options for a confirmation dialog: no fields, only a confirm / cancel choice.
 */
interface ConfirmationDialogOptions extends BaseDialogOptions {
  /**
   * Handler invoked once the user confirms. The caller owns the full workflow.
   */
  onConfirm: () => void;
}

/**
 * Composable centralizing the lifecycle dialog plumbing shared by the account
 * and organizational unit details pages.
 *
 * It is intentionally limited to opening dialogs and wiring their UI-related
 * properties (title, content, fields, namespace, pre-filled values). The
 * `onSubmit` / `onConfirm` handlers are provided by the caller, so each action
 * keeps full and explicit control over what happens on confirmation (update
 * call, notifications, error handling, side effects).
 * @param defaultNamespace - Default UI design namespace forwarded to the dialog.
 * @returns Helpers to open a form or a confirmation lifecycle dialog.
 */
export function useLifecycleDialogs(defaultNamespace: string) {
  const globalT = (getI18nInstance().global as Composer).t;

  /**
   * Opens a form dialog whose fields the user fills before confirming.
   * @template TForm - The flat form payload type emitted on submit.
   * @param dialog - The form dialog options.
   */
  function openFormDialog<TForm>(dialog: FormDialogOptions<TForm>): void {
    uiEventSubject.next({
      key: 'form',
      data: {
        type: 'open',
        title: globalT(`${dialog.i18nScope}.title`),
        content: globalT(`${dialog.i18nScope}.content`),
        uiNamespace: dialog.uiNamespace ?? defaultNamespace,
        i18nScope: dialog.i18nScope,
        formFields: dialog.formFields,
        initialFormData: dialog.initialFormData,
        onSubmit: dialog.onSubmit,
      },
    } as UiEvent);
  }

  /**
   * Opens a confirmation dialog (no fields).
   * @param dialog - The confirmation dialog options.
   */
  function openConfirmationDialog(dialog: ConfirmationDialogOptions): void {
    uiEventSubject.next({
      key: 'confirmation',
      data: {
        type: 'open',
        title: globalT(`${dialog.i18nScope}.title`),
        content: globalT(`${dialog.i18nScope}.content`),
        uiNamespace: dialog.uiNamespace ?? defaultNamespace,
        i18nScope: dialog.i18nScope,
        onConfirm: dialog.onConfirm,
      },
    });
  }

  return { openFormDialog, openConfirmationDialog };
}
