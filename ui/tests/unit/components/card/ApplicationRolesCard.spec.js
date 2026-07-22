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
import {
  getApplicationRoles,
  updateApplicationRoles,
} from 'src/services/ApplicationService';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ApplicationRolesCard from '../../../../src/components/card/ApplicationRolesCard.vue';

const mockedGetApplicationRoles = vi.mocked(getApplicationRoles);
const mockedUpdateApplicationRoles = vi.mocked(updateApplicationRoles);

const { mockNotify, mockUiEventSubjectNext, mockGlobalT, mockScopedT } =
  vi.hoisted(() => ({
    mockNotify: vi.fn(),
    mockUiEventSubjectNext: vi.fn(),
    mockGlobalT: vi.fn((v) => v),
    mockScopedT: vi.fn((v) => v),
  }));

vi.mock('@linagora/linid-im-front-corelib', () => ({
  loadAsyncComponent: vi.fn(() => null),
  useNotify: () => ({
    Notify: mockNotify,
  }),
  useScopedI18n: () => ({
    t: mockScopedT,
  }),
  useUiDesign: () => ({
    ui: vi.fn(() => ({})),
  }),
  getI18nInstance: vi.fn(() => ({ global: { t: mockGlobalT } })),
  uiEventSubject: { next: mockUiEventSubjectNext },
}));

vi.mock('axios', () => ({
  default: {
    isAxiosError: (err) => err?.isAxiosError === true,
  },
}));

vi.mock('src/services/ApplicationService', () => ({
  getApplicationRoles: vi.fn(),
  updateApplicationRoles: vi.fn(),
}));

vi.mock('boot/config', () => ({
  appConfig: {
    applicationRoleFields: [],
  },
}));

describe('Test component: ApplicationRolesCard', () => {
  let wrapper;

  const defaultProps = {
    applicationId: 'test-application-id',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetApplicationRoles.mockResolvedValue([
      { name: 'admin', description: 'Grants full administrative access' },
      { name: 'user' },
    ]);
  });

  describe('Test function: loadRoles', () => {
    it('should retrieve the application roles and expose them', async () => {
      wrapper = shallowMount(ApplicationRolesCard, { props: defaultProps });

      await flushPromises();

      expect(getApplicationRoles).toHaveBeenCalledWith('test-application-id');
      expect(wrapper.vm.roles).toEqual([
        { name: 'admin', description: 'Grants full administrative access' },
        { name: 'user' },
      ]);
      expect(wrapper.vm.isLoading).toBe(false);
    });

    it('should notify with the load error message on failure', async () => {
      mockedGetApplicationRoles.mockRejectedValueOnce(new Error('boom'));

      wrapper = shallowMount(ApplicationRolesCard, { props: defaultProps });
      await flushPromises();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'roles.errors.load',
      });
      expect(wrapper.vm.roles).toEqual([]);
      expect(wrapper.vm.isLoading).toBe(false);
    });
  });

  describe('Test computed: roleRows', () => {
    it('should map roles to rows with an empty description fallback', async () => {
      wrapper = shallowMount(ApplicationRolesCard, { props: defaultProps });
      await flushPromises();

      expect(wrapper.vm.roleRows).toEqual([
        { name: 'admin', description: 'Grants full administrative access' },
        { name: 'user', description: '' },
      ]);
    });
  });

  describe('Test function: openCreateRoleDialog', () => {
    beforeEach(async () => {
      wrapper = shallowMount(ApplicationRolesCard, { props: defaultProps });
      await flushPromises();
    });

    it('should open a form dialog with correct title and content', () => {
      wrapper.vm.openCreateRoleDialog();

      const { data } = mockUiEventSubjectNext.mock.calls[0][0];

      expect(data.title).toBe('ApplicationRoleActions.FormDialog.create.title');
      expect(data.content).toBe(
        'ApplicationRoleActions.FormDialog.create.content'
      );
    });

    it('should persist the full roles list with the new role appended on submit', async () => {
      const updated = [
        { name: 'admin', description: 'Grants full administrative access' },
        { name: 'user' },
        { name: 'auditor', description: 'Read-only access' },
      ];
      mockedUpdateApplicationRoles.mockResolvedValue(updated);

      wrapper.vm.openCreateRoleDialog();
      const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

      await onSubmit({ name: ' auditor ', description: ' Read-only access ' });

      expect(updateApplicationRoles).toHaveBeenCalledWith(
        'test-application-id',
        [
          { name: 'admin', description: 'Grants full administrative access' },
          { name: 'user' },
          { name: 'auditor', description: 'Read-only access' },
        ]
      );
      expect(wrapper.vm.roles).toEqual(updated);
      expect(getApplicationRoles).toHaveBeenCalledTimes(1);
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'positive',
        message: 'roles.createSuccess',
      });
    });

    it('should omit the description when it is left empty', async () => {
      mockedUpdateApplicationRoles.mockResolvedValue([]);

      wrapper.vm.openCreateRoleDialog();
      const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

      await onSubmit({ name: 'auditor', description: '   ' });

      expect(updateApplicationRoles).toHaveBeenCalledWith(
        'test-application-id',
        expect.arrayContaining([{ name: 'auditor' }])
      );
    });

    it('should notify and reject without calling the API when the name is already used', () => {
      wrapper.vm.openCreateRoleDialog();
      const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

      expect(() => onSubmit({ name: 'admin' })).toThrow();

      expect(updateApplicationRoles).not.toHaveBeenCalled();
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'roles.errors.duplicateName',
      });
    });
  });

  describe('Test function: openEditRoleDialog', () => {
    beforeEach(async () => {
      wrapper = shallowMount(ApplicationRolesCard, { props: defaultProps });
      await flushPromises();
    });

    it('should open a form dialog pre-filled with the edited role', () => {
      wrapper.vm.openEditRoleDialog('admin');

      const { data } = mockUiEventSubjectNext.mock.calls[0][0];

      expect(data.title).toBe('ApplicationRoleActions.FormDialog.edit.title');
      expect(data.initialFormData).toEqual({
        name: 'admin',
        description: 'Grants full administrative access',
      });
    });

    it('should do nothing when the role does not exist', () => {
      wrapper.vm.openEditRoleDialog('unknown');

      expect(mockUiEventSubjectNext).not.toHaveBeenCalled();
    });

    it('should persist the full roles list with the edited role replaced on submit', async () => {
      const updated = [
        { name: 'administrator', description: 'Renamed role' },
        { name: 'user' },
      ];
      mockedUpdateApplicationRoles.mockResolvedValue(updated);

      wrapper.vm.openEditRoleDialog('admin');
      const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

      await onSubmit({ name: 'administrator', description: 'Renamed role' });

      expect(updateApplicationRoles).toHaveBeenCalledWith(
        'test-application-id',
        [
          { name: 'administrator', description: 'Renamed role' },
          { name: 'user' },
        ]
      );
      expect(wrapper.vm.roles).toEqual(updated);
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'positive',
        message: 'roles.editSuccess',
      });
    });

    it('should allow keeping the same name when editing a role', async () => {
      mockedUpdateApplicationRoles.mockResolvedValue([]);

      wrapper.vm.openEditRoleDialog('admin');
      const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

      await onSubmit({ name: 'admin', description: 'Updated description' });

      expect(updateApplicationRoles).toHaveBeenCalledWith(
        'test-application-id',
        [
          { name: 'admin', description: 'Updated description' },
          { name: 'user' },
        ]
      );
    });

    it('should notify and reject when renaming to an already used name', () => {
      wrapper.vm.openEditRoleDialog('admin');
      const { onSubmit } = mockUiEventSubjectNext.mock.calls[0][0].data;

      expect(() => onSubmit({ name: 'user' })).toThrow();

      expect(updateApplicationRoles).not.toHaveBeenCalled();
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'roles.errors.duplicateName',
      });
    });
  });

  describe('Test function: openDeleteRoleDialog', () => {
    beforeEach(async () => {
      wrapper = shallowMount(ApplicationRolesCard, { props: defaultProps });
      await flushPromises();
    });

    it('should open a confirmation dialog with correct title and content', () => {
      wrapper.vm.openDeleteRoleDialog('admin');

      const event = mockUiEventSubjectNext.mock.calls[0][0];

      expect(event.key).toBe('confirmation');
      expect(event.data.title).toBe(
        'ApplicationRoleActions.ConfirmationDialog.delete.title'
      );
      expect(event.data.content).toBe(
        'ApplicationRoleActions.ConfirmationDialog.delete.content'
      );
    });

    it('should persist the full roles list without the deleted role on confirm', async () => {
      const updated = [{ name: 'user' }];
      mockedUpdateApplicationRoles.mockResolvedValue(updated);

      wrapper.vm.openDeleteRoleDialog('admin');
      const { onConfirm } = mockUiEventSubjectNext.mock.calls[0][0].data;

      await onConfirm();

      expect(updateApplicationRoles).toHaveBeenCalledWith(
        'test-application-id',
        [{ name: 'user' }]
      );
      expect(wrapper.vm.roles).toEqual(updated);
      expect(getApplicationRoles).toHaveBeenCalledTimes(1);
      expect(mockNotify).toHaveBeenCalledWith({
        type: 'positive',
        message: 'roles.deleteSuccess',
      });
    });
  });

  describe('Test function: saveRoles', () => {
    beforeEach(async () => {
      wrapper = shallowMount(ApplicationRolesCard, { props: defaultProps });
      await flushPromises();
    });

    it('should toggle isSavingRoles during the update', async () => {
      mockedUpdateApplicationRoles.mockResolvedValue([]);

      const savePromise = wrapper.vm.saveRoles([], 'roles.deleteSuccess');
      expect(wrapper.vm.isSavingRoles).toBe(true);

      await savePromise;
      expect(wrapper.vm.isSavingRoles).toBe(false);
    });

    it('should notify with the backend error message on axios error and rethrow', async () => {
      mockedUpdateApplicationRoles.mockRejectedValueOnce({
        isAxiosError: true,
        response: { data: { error: 'Backend validation error' } },
      });

      await expect(
        wrapper.vm.saveRoles([], 'roles.deleteSuccess')
      ).rejects.toBeDefined();

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'Backend validation error',
      });
      expect(wrapper.vm.isSavingRoles).toBe(false);
    });

    it('should notify with the generic update error message on unexpected error', async () => {
      mockedUpdateApplicationRoles.mockRejectedValueOnce(new Error('boom'));

      await expect(
        wrapper.vm.saveRoles([], 'roles.deleteSuccess')
      ).rejects.toThrow('boom');

      expect(mockNotify).toHaveBeenCalledWith({
        type: 'negative',
        message: 'roles.errors.update',
      });
    });
  });
});
