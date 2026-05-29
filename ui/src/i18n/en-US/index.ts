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

/**
 * Type definition for i18n messages.
 * This must match the structure in public/i18n/en-US.json for proper type checking.
 *
 * Note: Only core application translations are defined here.
 * Module-specific translations (e.g., moduleUsers) are loaded dynamically
 * from the public/i18n/*.json files.
 */
export default {
  application: {
    title: 'LinID - Identity Manager',
    version: 'Development version',
    dateTimeFormat: 'YYYY/MM/DD hh:mm:ss A',
    dateFormat: 'YYYY/MM/DD',
  },
  Homepage: {
    title: 'text',
    intro: 'text',
    opensource: 'text',
    license: 'text',
    links: 'text',
    branding: 'text',
  },
  AuthenticationCallbackPage: {
    processing: 'Processing authentication response...',
  },
  AccountStatusBadge: {
    active: 'Active',
    inactive: 'Inactive',
  },
  AccountDeactivatedInfoText: {
    message: 'This account will be deactivated on {date}.',
  },
  AccountSuspendedInfoText: {
    message: 'This account will be suspended on {date}',
  },
  AccountNotActivatedInfoText: {
    message: 'This user has not activated their account yet.',
  },
  AccountActivationActions: {
    DropdownButton: {
      title: 'Activation',
      activation: {
        immediate: 'Immediate',
        scheduled: 'Scheduled',
      },
    },
    ConfirmationDialog: {
      immediate: {
        title: 'Immediate Account Activation',
        content: 'Are you sure you want to activate this account immediately?',
        ButtonsCard: {
          confirm: 'Activate',
          cancel: 'Cancel',
        },
      },
    },
    FormDialog: {
      scheduled: {
        title: 'Schedule Account Activation',
        content: 'Select a start date for activating this account.',
        fields: {
          validityPeriodStart: {
            label: 'Activation date',
            close: 'Close',
            validation: {
              required: 'This field is required.',
              invalidDate: 'Invalid date format. Expected format is {format}.',
              afterDate: 'The date cannot be before today.',
            },
          },
        },
        ButtonsCard: {
          confirm: 'Schedule',
          cancel: 'Cancel',
        },
      },
    },
  },
  AccountSuspensionActions: {
    DropdownButton: {
      title: 'Suspension',
      suspension: {
        immediate: 'Immediate',
        scheduled: 'Scheduled',
      },
    },
  },
  AccountDeactivationActions: {
    DropdownButton: {
      title: 'Deactivation',
      deactivation: {
        immediate: 'Immediate',
        scheduled: 'Scheduled',
        modify: 'Modify',
      },
    },
  },
  AccountsPage: {
    menuTitle: 'Accounts',
    title: 'Accounts',
    detailButton: 'Details',
    ButtonsCard: {
      create: 'Create',
    },
    AdvancedSearchCard: {
      fields: {
        firstname: {
          label: 'First name',
          hint: 'Enter a first name to filter accounts.',
        },
        lastname: {
          label: 'Last name',
          hint: 'Enter a last name to filter accounts.',
        },
        email: {
          label: 'Email',
          hint: 'Enter an email to filter accounts.',
        },
        createdBy: {
          label: 'Created by',
          hint: 'Enter the name of the user who created the account to filter accounts.',
        },
        insertDate: {
          close: 'Close',
          label: 'Insert date',
          hint: 'Select an insert date to filter accounts.',
        },
      },
    },
    accountColumns: {
      firstname: 'First name',
      lastname: 'Last name',
      email: 'Email',
      createdBy: 'Created by',
      insertDate: 'Insert date',
      actions: 'Actions',
    },
    GenericEntityTable: {
      rowsPerPage: 'Rows per page',
      paginationLabel: '{start}-{end} on {total}',
    },
    errors: {
      notFound: 'Accounts not found',
      generic: 'Unable to load the accounts. Please try again later.',
    },
  },
  AccountCreationPage: {
    title: 'Create a new account',
    fields: {
      externalId: 'External ID',
      lastname: 'Last name',
      firstname: 'First name',
      email: 'Email',
    },
    validation: {
      required: 'This field is required',
      email: 'Invalid email format',
      invalidDate: 'Invalid date format. Expected format is {format}',
      fromDate: 'The date cannot be before today.',
    },
    ButtonsCard: {
      cancel: 'Cancel',
      confirm: 'Create',
      confirmLoading: 'Creating...',
    },
    success: 'Account successfully created',
    errors: {
      validation: 'Invalid data. Please check the form.',
      generic: 'Unable to create the account. Please try again later.',
    },
  },
  AccountDetailsPage: {
    title: 'Account details',
    EntityDetailsCard: {
      title: 'Account information',
      attributes: {
        firstname: 'First name',
        lastname: 'Last name',
        email: 'Email',
        createdBy: 'Created by',
        updatedBy: 'Updated by',
        insertDate: 'Creation date',
        updateDate: 'Last update',
      },
    },
    ButtonsCard: {
      cancel: 'Back',
    },
    updateStatusSuccess: 'Account status successfully updated',
    immediateActivationSuccess: 'The account can be activated within one hour',
    scheduledActivationSuccess: 'The account can be activated from {date}',
    errors: {
      notFound: 'Account not found',
      generic: 'Unable to load the account. Please try again later.',
      status: 'Unable to update the account status. Please try again later.',
    },
  },
  AccountDeactivatedBanner: {
    content: 'This account has been deactivated since {date}.',
    reactivateImmediateButton: 'Immediate reactivation',
    reactivateScheduledButton: 'Schedule reactivation',
  },
  AccountDeactivatedWarningBanner: {
    content:
      'This account will be deactivated tomorrow (on {date}) | This account will be deactivated in {count} days (on {date})',
    deactivateImmediateButton: 'Immediate deactivation',
    modifyDeactivationButton: 'Modify deactivation date',
  },
  AccountSuspendedBanner: {
    content: 'This account has been suspended since {date}.',
    contentWithEndDate:
      'This account has been suspended from {date} until {endDate}.',
    clearSuspensionButton: 'Immediate reactivation',
    modifySuspensionButton: 'Adjust suspension settings',
  },
};
