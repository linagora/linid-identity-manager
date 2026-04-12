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
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AuthenticationCallbackPage from '../../../src/pages/AuthenticationCallbackPage.vue';

const {
  mockReplace,
  mockSigninRedirectCallback,
  mockSignoutRedirectCallback,
  mockRemoveUser,
} = vi.hoisted(() => ({
  mockReplace: vi.fn(),
  mockSigninRedirectCallback: vi.fn(),
  mockSignoutRedirectCallback: vi.fn(),
  mockRemoveUser: vi.fn(),
}));

let mockRoutePath = '/callback';

vi.mock('vue-router', () => ({
  useRouter: () => ({
    replace: mockReplace,
  }),
  useRoute: () => ({
    path: mockRoutePath,
  }),
}));

vi.mock('@linagora/linid-im-front-corelib', () => ({
  useScopedI18n: () => ({
    t: vi.fn((v) => v),
  }),
}));

vi.mock('../../../src/boot/oidc', () => ({
  oidcClient: {
    signinRedirectCallback: mockSigninRedirectCallback,
    signoutRedirectCallback: mockSignoutRedirectCallback,
    removeUser: mockRemoveUser,
  },
}));

vi.mock('#q-app/wrappers', () => ({
  defineBoot: vi.fn(),
  defineRoute: vi.fn(),
  defineStore: vi.fn(),
}));

describe('Test component: AuthenticationCallbackPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockRoutePath = '/callback';
  });

  const mountComponent = async () => {
    shallowMount(AuthenticationCallbackPage);
    await flushPromises();
  };

  describe('Test hook: onMounted', () => {
    it('should call signinRedirectCallback on mount', async () => {
      mockSigninRedirectCallback.mockResolvedValue({ state: {} });

      await mountComponent();

      expect(mockSigninRedirectCallback).toHaveBeenCalled();
    });

    it('should not call signoutRedirectCallback for sign-in callback', async () => {
      mockSigninRedirectCallback.mockResolvedValue({ state: {} });

      await mountComponent();

      expect(mockSignoutRedirectCallback).not.toHaveBeenCalled();
    });

    it('should redirect to state.redirectUrl after sign-in', async () => {
      mockSigninRedirectCallback.mockResolvedValue({
        state: { redirectUrl: '/dashboard' },
      });

      await mountComponent();

      expect(mockReplace).toHaveBeenCalledWith('/dashboard');
    });

    it('should redirect to / when state has no redirectUrl', async () => {
      mockSigninRedirectCallback.mockResolvedValue({ state: {} });

      await mountComponent();

      expect(mockReplace).toHaveBeenCalledWith('/');
    });

    it('should redirect to / when state is undefined', async () => {
      mockSigninRedirectCallback.mockResolvedValue({});

      await mountComponent();

      expect(mockReplace).toHaveBeenCalledWith('/');
    });

    it('should redirect to / when redirectUrl is an absolute URL', async () => {
      mockSigninRedirectCallback.mockResolvedValue({
        state: { redirectUrl: 'https://evil.com' },
      });

      await mountComponent();

      expect(mockReplace).toHaveBeenCalledWith('/');
    });

    it('should redirect to / when redirectUrl is protocol-relative', async () => {
      mockSigninRedirectCallback.mockResolvedValue({
        state: { redirectUrl: '//evil.com' },
      });

      await mountComponent();

      expect(mockReplace).toHaveBeenCalledWith('/');
    });

    it('should call signoutRedirectCallback when path ends with /callback/logout', async () => {
      mockRoutePath = '/callback/logout';
      mockSignoutRedirectCallback.mockResolvedValue();
      mockRemoveUser.mockResolvedValue();

      await mountComponent();

      expect(mockSignoutRedirectCallback).toHaveBeenCalled();
    });

    it('should call removeUser after signoutRedirectCallback', async () => {
      mockRoutePath = '/callback/logout';
      mockSignoutRedirectCallback.mockResolvedValue();
      mockRemoveUser.mockResolvedValue();

      await mountComponent();

      expect(mockRemoveUser).toHaveBeenCalled();
    });

    it('should not call signinRedirectCallback for sign-out callback', async () => {
      mockRoutePath = '/callback/logout';
      mockSignoutRedirectCallback.mockResolvedValue();
      mockRemoveUser.mockResolvedValue();

      await mountComponent();

      expect(mockSigninRedirectCallback).not.toHaveBeenCalled();
    });

    it('should redirect to / after sign-out', async () => {
      mockRoutePath = '/callback/logout';
      mockSignoutRedirectCallback.mockResolvedValue();
      mockRemoveUser.mockResolvedValue();

      await mountComponent();

      expect(mockReplace).toHaveBeenCalledWith('/');
    });

    it('should redirect to / when signinRedirectCallback throws', async () => {
      mockSigninRedirectCallback.mockRejectedValue(new Error('OIDC error'));

      await mountComponent();

      expect(mockReplace).toHaveBeenCalledWith('/');
    });

    it('should redirect to / when signoutRedirectCallback throws', async () => {
      mockRoutePath = '/callback/logout';
      mockSignoutRedirectCallback.mockRejectedValue(new Error('OIDC error'));

      await mountComponent();

      expect(mockReplace).toHaveBeenCalledWith('/');
    });

    it('should log error to console when callback fails', async () => {
      const consoleSpy = vi
        .spyOn(console, 'error')
        .mockImplementation(() => {});
      const error = new Error('OIDC error');
      mockSigninRedirectCallback.mockRejectedValue(error);

      await mountComponent();

      expect(consoleSpy).toHaveBeenCalledWith(
        'Error during OIDC callback processing:',
        error
      );
      consoleSpy.mockRestore();
    });
  });
});
