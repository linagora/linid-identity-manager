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

import bootFn, { getUser } from 'src/boot/oidc';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockGetUser, mockSigninSilent } = vi.hoisted(() => ({
  mockGetUser: vi.fn(),
  mockSigninSilent: vi.fn(),
}));

vi.mock('oidc-client-ts', () => ({
  UserManager: vi.fn().mockImplementation(function () {
    this.getUser = mockGetUser;
    this.signinSilent = mockSigninSilent;
    this.events = { addUserLoaded: vi.fn() };
  }),
}));

vi.mock('@linagora/linid-im-front-corelib', () => ({
  useLinidUserStore: () => ({
    setUserFromClaims: vi.fn(),
  }),
}));

describe('Test boot: oidc', () => {
  beforeEach(async () => {
    vi.clearAllMocks();
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () =>
        Promise.resolve({ authority: 'http://localhost', client_id: 'test' }),
    });
    await bootFn({ app: { config: { globalProperties: {} } } });
  });

  describe('Test function: getUser', () => {
    it('should return the user when it is not expired', async () => {
      const user = { expired: false };
      mockGetUser.mockResolvedValue(user);

      const result = await getUser();

      expect(result).toBe(user);
      expect(mockSigninSilent).not.toHaveBeenCalled();
    });

    it('should return null when no user is logged in', async () => {
      mockGetUser.mockResolvedValue(null);

      const result = await getUser();

      expect(result).toBeNull();
      expect(mockSigninSilent).not.toHaveBeenCalled();
    });

    it('should attempt a silent refresh and return the refreshed user when user is expired', async () => {
      const expiredUser = { expired: true };
      const refreshedUser = { expired: false };
      mockGetUser.mockResolvedValue(expiredUser);
      mockSigninSilent.mockResolvedValue(refreshedUser);

      const result = await getUser();

      expect(mockSigninSilent).toHaveBeenCalledTimes(1);
      expect(result).toBe(refreshedUser);
    });

    it('should return null and warn when the silent refresh fails', async () => {
      const expiredUser = { expired: true };
      mockGetUser.mockResolvedValue(expiredUser);
      mockSigninSilent.mockRejectedValue(new Error('refresh failed'));
      const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      const result = await getUser();

      expect(result).toBeNull();
      expect(warnSpy).toHaveBeenCalledWith(
        'Silent OIDC refresh failed',
        expect.any(Error)
      );
    });

    it('should call signinSilent only once when getUser() is invoked twice concurrently on an expired user', async () => {
      const expiredUser = { expired: true };
      const refreshedUser = { expired: false };
      mockGetUser.mockResolvedValue(expiredUser);
      mockSigninSilent.mockResolvedValue(refreshedUser);

      const [result1, result2] = await Promise.all([getUser(), getUser()]);

      expect(mockSigninSilent).toHaveBeenCalledTimes(1);
      expect(result1).toBe(refreshedUser);
      expect(result2).toBe(refreshedUser);
    });
  });
});
