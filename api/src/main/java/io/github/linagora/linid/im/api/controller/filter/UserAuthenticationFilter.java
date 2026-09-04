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

import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Account;
import io.github.linagora.linid.im.api.service.AccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filter that authenticates incoming HTTP requests based on JWT tokens.
 *
 * <p>This filter extracts the JWT from the security context, validates it, retrieves the corresponding
 * {@link UserPrincipal} and sets the authentication in the security context.
 *
 * <p>When no account matches the token, the request fails through the {@link AuthenticationEntryPoint}
 * with a {@code 401} response, as a bearer token rejected by the resource server would.
 *
 * <p>Extends {@link OncePerRequestFilter} to ensure that this filter is executed once per request.
 */
@RequiredArgsConstructor
public class UserAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Service used to retrieve Account.
     */
    private final AccountService accountService;

    /**
     * Entry point producing the {@code 401} response when no account matches the token.
     */
    private final AuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain)
        throws ServletException, IOException {
        String email = emailOf(SecurityContextHolder.getContext().getAuthentication());
        Optional<Account> account = Optional.ofNullable(email)
            .filter(StringUtils::isNotBlank)
            .flatMap(accountService::getAccountByEmail);

        if (account.isEmpty()) {
            SecurityContextHolder.clearContext();
            entryPoint.commence(request, response, new InvalidBearerTokenException("No account matches the token"));
            return;
        }

        UserPrincipal user = new UserPrincipal();
        user.setId(account.get().getId());
        user.setEmail(email);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, List.of())
        );

        filterChain.doFilter(request, response);
    }

    /**
     * Returns the {@code email} claim of the JWT held by the given authentication.
     *
     * @param authentication the current authentication, may be {@code null}
     * @return the email claim, or {@code null} when the principal is not a JWT
     */
    private static String emailOf(final Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("email");
        }

        return null;
    }

}
