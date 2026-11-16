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
import lombok.extern.slf4j.Slf4j;
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
 * <p>This filter extracts the JWT from the security context, resolves the account whose {@code external_id}
 * matches its {@code sub} claim and sets the corresponding {@link UserPrincipal} in the security context.
 * The account is never resolved from the {@code email} claim: some identity providers let users change their own
 * email address, which would let them authenticate as the account holding it, whereas only the identity provider
 * sets the subject.
 *
 * <p>When a token is presented but no account matches it, the request fails through the
 * {@link AuthenticationEntryPoint} with a {@code 401} response, as a bearer token rejected by the resource
 * server would. A warning with the subject of the token is logged, since an {@code external_id} that differs
 * from the subject at the identity provider would otherwise end in a {@code 401} that leaves no trace. Without
 * credentials the request goes down the chain, which issues the standard challenge.
 *
 * <p>Extends {@link OncePerRequestFilter} to ensure that this filter is executed once per request.
 */
@Slf4j
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
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            // No credentials presented: the chain issues the standard "Bearer" challenge.
            filterChain.doFilter(request, response);
            return;
        }

        Optional<String> subject = subjectOf(authentication).filter(StringUtils::isNotBlank);
        Optional<Account> account = subject.flatMap(accountService::getAccountByExternalId);

        if (account.isEmpty()) {
            log.warn("No account matches the subject of the access token (sub={})", subject.orElse(null));
            SecurityContextHolder.clearContext();
            entryPoint.commence(request, response, new InvalidBearerTokenException(""));
            return;
        }

        UserPrincipal user = new UserPrincipal();
        user.setId(account.get().getId());
        user.setEmail(account.get().getEmail());

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, List.of())
        );

        filterChain.doFilter(request, response);
    }

    /**
     * Returns the {@code sub} claim of the JWT held by the given authentication.
     *
     * @param authentication the current authentication
     * @return the subject, empty when the principal is not a JWT or the claim is missing
     */
    private static Optional<String> subjectOf(final Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.ofNullable(jwt.getSubject());
        }

        return Optional.empty();
    }

}
