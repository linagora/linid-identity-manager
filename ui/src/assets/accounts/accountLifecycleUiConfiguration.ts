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

export const accountStatusReasons = {
  suspension: {
    reasons: [
      { value: 'Suspension Reason A' },
      { value: 'Suspension Reason B' },
      { value: 'Suspension Reason C' },
    ],
    subreasons: [
      { value: 'Suspension Sub-reason A.1', reason: 'Suspension Reason A' },
      { value: 'Suspension Sub-reason A.2', reason: 'Suspension Reason A' },
      { value: 'Suspension Sub-reason B.1', reason: 'Suspension Reason B' },
      { value: 'Suspension Sub-reason C.1', reason: 'Suspension Reason C' },
    ],
  },
  deactivation: {
    reasons: [
      { value: 'Deactivation Reason A' },
      { value: 'Deactivation Reason B' },
      { value: 'Deactivation Reason C' },
    ],
    subreasons: [
      { value: 'Deactivation Sub-reason A.1', reason: 'Deactivation Reason A' },
      { value: 'Deactivation Sub-reason A.2', reason: 'Deactivation Reason A' },
      { value: 'Deactivation Sub-reason B.1', reason: 'Deactivation Reason B' },
      { value: 'Deactivation Sub-reason B.2', reason: 'Deactivation Reason B' },
      { value: 'Deactivation Sub-reason B.3', reason: 'Deactivation Reason B' },
      { value: 'Deactivation Sub-reason C.1', reason: 'Deactivation Reason C' },
      { value: 'Deactivation Sub-reason C.2', reason: 'Deactivation Reason C' },
      { value: 'Deactivation Sub-reason C.3', reason: 'Deactivation Reason C' },
    ],
  },
};

export const accountLifecycleUiConfiguration = {
  'suspension.immediate': [
    {
      name: 'statusReason',
      type: 'String',
      input: 'List',
      required: true,
      hasValidations: false,
      inputSettings: {
        values: accountStatusReasons['suspension'].reasons.map(
          (reason) => reason.value
        ),
      },
    },
    {
      name: 'statusSubreason',
      type: 'String',
      input: 'List',
      required: true,
      hasValidations: false,
      inputSettings: {
        values: accountStatusReasons['suspension'].subreasons.map(
          (subreason) => subreason.value
        ),
      },
    },
    {
      name: 'statusComment',
      type: 'String',
      input: 'TextArea',
      required: false,
      hasValidations: false,
      inputSettings: {},
    },
  ],
  'deactivation.immediate': [
    {
      name: 'statusReason',
      type: 'String',
      input: 'List',
      required: true,
      hasValidations: false,
      inputSettings: {
        values: accountStatusReasons['deactivation'].reasons.map(
          (reason) => reason.value
        ),
      },
    },
    {
      name: 'statusSubreason',
      type: 'String',
      input: 'List',
      required: true,
      hasValidations: false,
      inputSettings: {
        values: accountStatusReasons['deactivation'].subreasons.map(
          (subreason) => subreason.value
        ),
      },
    },
    {
      name: 'statusComment',
      type: 'String',
      input: 'TextArea',
      required: false,
      hasValidations: false,
      inputSettings: {},
    },
  ],
  'reactivation.immediate': [
    {
      name: 'statusComment',
      type: 'String',
      input: 'TextArea',
      required: true,
      hasValidations: false,
      inputSettings: {},
    },
  ],
  'activation.scheduled': [
    {
      name: 'validityPeriodStart',
      type: 'String',
      required: true,
      hasValidations: false,
      input: 'Date',
      inputSettings: {
        mask: '{{ t("application.dateFormat") }}',
        options: {
          afterDate: '{{ today }}',
        },
      },
    },
  ],
  'revalidation.scheduled': [
    {
      name: 'validityPeriodEnd',
      type: 'String',
      required: true,
      input: 'Date',
      inputSettings: {
        mask: '{{ t("application.dateFormat") }}',
        options: {
          afterDate: '{{ today }}',
        },
      },
    },
  ],
  'deactivation.scheduled': [
    {
      name: 'validityPeriodEnd',
      type: 'String',
      required: true,
      hasValidations: false,
      input: 'Date',
      inputSettings: {
        mask: '{{ t("application.dateFormat") }}',
        options: {
          afterDate: '{{ today }}',
        },
      },
    },
    {
      name: 'statusReason',
      type: 'String',
      input: 'List',
      required: true,
      hasValidations: false,
      inputSettings: {
        values: accountStatusReasons['deactivation'].reasons.map(
          (reason) => reason.value
        ),
      },
    },
    {
      name: 'statusSubreason',
      type: 'String',
      input: 'List',
      required: true,
      hasValidations: false,
      inputSettings: {
        values: accountStatusReasons['deactivation'].subreasons.map(
          (subreason) => subreason.value
        ),
      },
    },
    {
      name: 'statusComment',
      type: 'String',
      input: 'TextArea',
      required: false,
      hasValidations: false,
      inputSettings: {},
    },
  ],
  'deactivation.modify': [
    {
      name: 'validityPeriodEnd',
      type: 'String',
      required: true,
      hasValidations: false,
      input: 'Date',
      inputSettings: {
        mask: '{{ t("application.dateFormat") }}',
        options: {
          afterDate: '{{ today }}',
        },
      },
    },
    {
      name: 'statusReason',
      type: 'String',
      required: true,
      hasValidations: false,
      input: 'List',
      inputSettings: {
        values: accountStatusReasons['deactivation'].reasons.map(
          (reason) => reason.value
        ),
      },
    },
    {
      name: 'statusSubreason',
      type: 'String',
      required: true,
      hasValidations: false,
      input: 'List',
      inputSettings: {
        values: accountStatusReasons['deactivation'].subreasons.map(
          (subreason) => subreason.value
        ),
      },
    },
    {
      name: 'statusComment',
      type: 'String',
      input: 'TextArea',
      required: false,
      hasValidations: false,
      inputSettings: {},
    },
  ],
  'suspension.modify': [
    {
      name: 'suspensionPeriodStart',
      type: 'String',
      input: 'Date',
      required: false,
      hasValidations: false,
      inputSettings: {
        mask: '{{ t("application.dateFormat") }}',
        options: {
          afterDate: '{{ today }}',
        },
      },
    },
    {
      name: 'suspensionPeriodEnd',
      type: 'String',
      input: 'Date',
      required: false,
      hasValidations: false,
      inputSettings: {
        mask: '{{ t("application.dateFormat") }}',
        options: {
          afterDate: '{{ today }}',
          fromDate: '{{ entity.suspensionPeriodStart }}',
        },
      },
    },
    {
      name: 'statusReason',
      type: 'String',
      input: 'List',
      required: true,
      hasValidations: false,
      inputSettings: {
        values: accountStatusReasons['suspension'].reasons.map(
          (reason) => reason.value
        ),
      },
    },
    {
      name: 'statusSubreason',
      type: 'String',
      input: 'List',
      required: true,
      hasValidations: false,
      inputSettings: {
        values: accountStatusReasons['suspension'].subreasons.map(
          (subreason) => subreason.value
        ),
      },
    },
    {
      name: 'statusComment',
      type: 'String',
      input: 'TextArea',
      required: false,
      hasValidations: false,
      inputSettings: {},
    },
  ],
  'suspension.scheduled': [
    {
      name: 'suspensionPeriodStart',
      type: 'String',
      required: true,
      hasValidations: false,
      input: 'Date',
      inputSettings: {
        mask: '{{ t("application.dateFormat") }}',
        options: {
          afterDate: '{{ today }}',
        },
      },
    },
    {
      name: 'suspensionPeriodEnd',
      type: 'String',
      input: 'Date',
      required: false,
      hasValidations: false,
      inputSettings: {
        mask: '{{ t("application.dateFormat") }}',
        options: {
          afterDate: '{{ today }}',
          fromDate: '{{ entity.suspensionPeriodStart }}',
        },
      },
    },
    {
      name: 'statusReason',
      type: 'String',
      input: 'List',
      required: true,
      hasValidations: false,
      inputSettings: {
        values: accountStatusReasons['suspension'].reasons.map(
          (reason) => reason.value
        ),
      },
    },
    {
      name: 'statusSubreason',
      type: 'String',
      input: 'List',
      required: true,
      hasValidations: false,
      inputSettings: {
        values: accountStatusReasons['suspension'].subreasons.map(
          (subreason) => subreason.value
        ),
      },
    },
    {
      name: 'statusComment',
      type: 'String',
      input: 'TextArea',
      required: false,
      hasValidations: false,
      inputSettings: {},
    },
  ],
};

/**
 * Shape of a single lifecycle dialog form field, inferred from the lifecycle UI
 * configurations. Shared by the account and organizational unit configurations,
 * which declare fields of the same shape, and consumed by the form dialog.
 */
export type LifecycleFormField =
  (typeof accountLifecycleUiConfiguration)[keyof typeof accountLifecycleUiConfiguration][number];
