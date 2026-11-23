/*
 * Copyright (C) 2020-2026 Linagora
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

package io.github.linagora.linid.im.api.controller.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.linagora.linid.im.api.config.SuperAdminConfig;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Account;
import io.github.linagora.linid.im.api.service.AccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("Test class: UserAuthenticationFilter")
class UserAuthenticationFilterTest {

    private UserAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private AccountService accountService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        filter = new UserAuthenticationFilter(accountService, new SuperAdminConfig("admin-sub"));
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(UserAuthenticationFilter.class)).addAppender(logAppender);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(UserAuthenticationFilter.class)).detachAppender(logAppender);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("test doFilterInternal: should authenticate the account matching the subject")
    void doFilterInternal_withKnownSubject_setsAuthenticationAndContinues() throws ServletException, IOException {
        UUID uuid = UUID.randomUUID();
        when(accountService.getAccountByExternalId("user-sub")).thenReturn(
            Optional.of(Account.builder().id(uuid).externalId("user-sub").email("user@example.com").build()));
        authenticateWith(jwt("user-sub", "user@example.com"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        UserPrincipal user = authenticatedUser();
        assertEquals(uuid, user.getId());
        assertEquals("user@example.com", user.getEmail());
        assertFalse(user.isSuperAdmin());
        assertTrue(logAppender.list.isEmpty());
    }

    @Test
    @DisplayName("test doFilterInternal: should mark the declared SUPER_ADMIN as such")
    void doFilterInternal_withSuperAdminSubject_marksTheUserAsSuperAdmin() throws ServletException, IOException {
        UUID uuid = UUID.randomUUID();
        when(accountService.getAccountByExternalId("admin-sub")).thenReturn(
            Optional.of(Account.builder().id(uuid).externalId("admin-sub").email("admin@example.com").build()));
        authenticateWith(jwt("admin-sub", "admin@example.com"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        UserPrincipal user = authenticatedUser();
        assertEquals(uuid, user.getId());
        assertTrue(user.isSuperAdmin());
    }

    @Test
    @DisplayName("test doFilterInternal: should ignore the email claim when resolving the account")
    void doFilterInternal_withEmailOfAnotherAccount_resolvesTheSubject() throws ServletException, IOException {
        UUID uuid = UUID.randomUUID();
        when(accountService.getAccountByExternalId("user-sub")).thenReturn(
            Optional.of(Account.builder().id(uuid).email("user@example.com").build()));
        // The token carries the email of another account: only its subject designates the account.
        authenticateWith(jwt("user-sub", "admin@example.com"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        UserPrincipal user = authenticatedUser();
        assertEquals(uuid, user.getId());
        assertEquals("user@example.com", user.getEmail());
        // No other lookup, such as a fallback on the email, may designate the account.
        verify(accountService, times(1)).getAccountByExternalId("user-sub");
        verifyNoMoreInteractions(accountService);
    }

    @Test
    @DisplayName("test doFilterInternal: should respond 401 on unknown subject")
    void doFilterInternal_withUnknownSubject_respondsUnauthorized() throws ServletException, IOException {
        when(accountService.getAccountByExternalId("unknown-sub")).thenReturn(Optional.empty());
        // A known email does not make up for an unknown subject.
        authenticateWith(jwt("unknown-sub", "admin@example.com"));

        filter.doFilterInternal(request, response, filterChain);

        assertUnauthorized();
        verify(accountService, times(1)).getAccountByExternalId("unknown-sub");
        // Without this warning, an external_id that differs from the subject would be a silent 401.
        assertEquals(1, logAppender.list.size());
        assertEquals(Level.WARN, logAppender.list.getFirst().getLevel());
        assertTrue(logAppender.list.getFirst().getFormattedMessage().contains("sub=unknown-sub"));
    }

    @Test
    @DisplayName("test doFilterInternal: should defer the challenge to the chain without credentials")
    void doFilterInternal_withNoAuthentication_defersToChain() throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(null);

        filter.doFilterInternal(request, response, filterChain);

        // The filter neither authenticates nor challenges: the rest of the chain rejects the request with 401.
        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(response.getHeader("WWW-Authenticate"));
        verify(accountService, never()).getAccountByExternalId(any());
    }

    @Test
    @DisplayName("test doFilterInternal: should respond 401 on invalid token")
    void doFilterInternal_withInvalidPrincipal_respondsUnauthorized() throws ServletException, IOException {
        // Principal is not a Jwt
        authenticateWith("not-a-jwt");

        filter.doFilterInternal(request, response, filterChain);

        assertUnauthorized();
        verify(accountService, never()).getAccountByExternalId(any());
    }

    @Test
    @DisplayName("test doFilterInternal: should respond 401 when the subject claim is missing")
    void doFilterInternal_withoutSubject_respondsUnauthorized() throws ServletException, IOException {
        authenticateWith(Jwt.withTokenValue("token").header("alg", "RS256")
            .claim("email", "admin@example.com").build());

        filter.doFilterInternal(request, response, filterChain);

        assertUnauthorized();
        verify(accountService, never()).getAccountByExternalId(any());
    }

    @Test
    @DisplayName("test doFilterInternal: should respond 401 when the subject claim is blank")
    void doFilterInternal_withBlankSubject_respondsUnauthorized() throws ServletException, IOException {
        authenticateWith(jwt("   ", "admin@example.com"));

        filter.doFilterInternal(request, response, filterChain);

        assertUnauthorized();
        verify(accountService, never()).getAccountByExternalId(any());
    }

    private static Jwt jwt(final String subject, final String email) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject(subject)
            .claim("email", email)
            .build();
    }

    private static void authenticateWith(final Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    private static UserPrincipal authenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        return assertInstanceOf(UserPrincipal.class, auth.getPrincipal());
    }

    private void assertUnauthorized() throws ServletException, IOException {
        assertEquals(401, response.getStatus());
        String challenge = response.getHeader("WWW-Authenticate");
        assertNotNull(challenge);
        assertTrue(challenge.startsWith("Bearer "));
        verify(filterChain, never()).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
