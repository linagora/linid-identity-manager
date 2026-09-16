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

package io.github.linagora.linid.im.api.config;

import io.github.linagora.linid.im.api.controller.filter.UserAuthenticationFilter;
import io.github.linagora.linid.im.api.service.AccountService;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTypeValidator;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the LinID-identity-manager API application.
 *
 * <p>This class defines the security filter chain based on the {@code auth.enabled} property,
 * allowing dynamic enabling/disabling of JWT authentication.
 *
 * <p>CSRF protection is disabled, and the application is stateless (no HTTP sessions).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Service used to retrieve Account.
     */
    private final AccountService accountService;

    /**
     * Public endpoints (no authentication, no custom filter).
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception in case of configuration errors
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicEndpoints(final HttpSecurity http) throws Exception {
        http.securityMatcher(
                "/health",
                "/actuator/**",
                "/i18n/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html")
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * Secured endpoints (JWT and custom filter).
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception in case of configuration errors
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securedEndpoints(final HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(
                session ->
                    session.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .addFilterAfter(
                new UserAuthenticationFilter(accountService), BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Validates the JOSE {@code typ} header of the bearer tokens.
     *
     * <p>Nothing in this code base calls this bean directly: Spring Boot collects every
     * {@link OAuth2TokenValidator} bean when it auto-configures the {@code JwtDecoder} from the
     * {@code spring.security.oauth2.resourceserver.jwt.*} properties, and adds it to the validators
     * applied once the signature has been verified. Because it is a {@link JwtTypeValidator}, it
     * replaces the default type validator, which accepts {@code JWT} or an absent header.
     *
     * <p>Each instance authenticates against a single OIDC provider, whose {@code typ} it must match. The
     * {@code spring.security.oauth2.resourceserver.jwt.expected-type} property carries that value,
     * compared case-insensitively, and defaults to {@code at+jwt} — the type RFC 9068 prescribes for JWT
     * access tokens, and the one LemonLDAP::NG emits ({@code at+JWT}). Against another provider, decode
     * one of its access tokens and read the {@code typ} header rather than assuming it follows the RFC.
     *
     * <p>This check defends against replaying an ID token as a bearer token: providers usually issue ID
     * tokens and access tokens with the same {@code aud} — the OIDC client ID — so audience validation
     * cannot separate them and the {@code typ} header is the only thing that does. Setting this property
     * to {@code JWT}, as a provider typing its access tokens that way would require, removes that
     * defence: it is a deliberate trade-off, not a neutral configuration change. A token carrying no
     * {@code typ} header is rejected ({@code 401}).
     *
     * @param expectedType the {@code typ} header value required on bearer tokens
     * @return the token type validator
     */
    @Bean
    public OAuth2TokenValidator<Jwt> accessTokenTypeValidator(
        @Value("${spring.security.oauth2.resourceserver.jwt.expected-type:at+jwt}") final String expectedType) {
        final String type = expectedType.trim();
        log.info("Bearer tokens must carry the JOSE typ header '{}'", type);
        return new JwtTypeValidator(type);
    }

    /**
     * Configures the OpenAPI documentation with a security scheme for bearer token authentication.
     *
     * <p>This configuration adds a security scheme named {@code bearerAuth} using the HTTP Bearer
     * authentication method with JWT tokens. It also registers a global security requirement so that
     * all endpoints in the Swagger UI require this authentication unless explicitly overridden.
     *
     * <p>This allows the Swagger UI to display an "Authorize" button where users can input their JWT
     * Bearer token, which will be sent as an {@code Authorization} header in subsequent requests.
     *
     * @return the configured {@link OpenAPI} instance for Swagger documentation
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .components(
                new Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
