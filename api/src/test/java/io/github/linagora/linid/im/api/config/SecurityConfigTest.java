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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.linagora.linid.im.api.controller.UserPreferenceController;
import io.github.linagora.linid.im.api.model.user.preference.UserPreferenceMapper;
import io.github.linagora.linid.im.api.persistence.model.Account;
import io.github.linagora.linid.im.api.service.AccountService;
import io.github.linagora.linid.im.api.service.UserPreferenceService;
import io.github.linagora.linid.im.corelib.i18n.I18nService;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.JwkSetUriJwtDecoderBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestOperations;

@WebMvcTest(controllers = UserPreferenceController.class)
@Import({SecurityConfig.class, SecurityConfigTest.JwkSetConfiguration.class})
@TestPropertySource(properties = {
    "AUTH_ISSUER_URI=" + SecurityConfigTest.ISSUER,
    "AUTH_JWK_SET_URI=" + SecurityConfigTest.ISSUER + "/oauth2/jwks",
    "AUTH_AUDIENCE=" + SecurityConfigTest.AUDIENCE
})
@DisplayName("Test class: SecurityConfig")
class SecurityConfigTest {

    static final String ISSUER = "https://auth.test";

    static final String AUDIENCE = "linid-im-client";

    private static final KeyPair KEY_PAIR = generateKeyPair();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private UserPreferenceService userPreferenceService;

    @MockitoBean
    private UserPreferenceMapper userPreferenceMapper;

    @MockitoBean
    private I18nService i18nService;

    /**
     * Serves the test JWK set instead of fetching it over HTTP; the decoder and its validators stay the
     * auto-configured ones.
     */
    @TestConfiguration
    static class JwkSetConfiguration {

        @Bean
        JwkSetUriJwtDecoderBuilderCustomizer jwkSetFromTestKey() {
            String jwkSet = new JWKSet(new RSAKey.Builder((RSAPublicKey) KEY_PAIR.getPublic()).keyID("test").build())
                .toString();
            RestOperations restOperations = Mockito.mock(RestOperations.class);
            Mockito.when(restOperations.exchange(Mockito.any(RequestEntity.class), Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.ok(jwkSet));
            return builder -> builder.restOperations(restOperations);
        }
    }

    @BeforeEach
    void setUp() {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        Mockito.when(accountService.getAccountByEmail("john@linid.org")).thenReturn(Optional.of(account));
        Mockito.when(userPreferenceService.findAll(Mockito.any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("test securedEndpoints: should accept an access token with the expected audience")
    void testAcceptsAccessToken() throws Exception {
        mockMvc.perform(get("/user-preferences").header("Authorization", "Bearer " + token("at+JWT", AUDIENCE)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("test securedEndpoints: should reject a token with a foreign audience")
    void testRejectsWrongAudience() throws Exception {
        mockMvc.perform(get("/user-preferences").header("Authorization", "Bearer " + token("at+JWT", "other-api")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("test securedEndpoints: should reject an ID token used as bearer")
    void testRejectsIdToken() throws Exception {
        mockMvc.perform(get("/user-preferences").header("Authorization", "Bearer " + token("JWT", AUDIENCE)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("test securedEndpoints: should reject a token without typ header")
    void testRejectsMissingType() throws Exception {
        mockMvc.perform(get("/user-preferences").header("Authorization", "Bearer " + token(null, AUDIENCE)))
            .andExpect(status().isUnauthorized());
    }

    private static String token(final String type, final String audience) throws Exception {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID("test")
            .type(type == null ? null : new JOSEObjectType(type))
            .build();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(ISSUER)
            .subject("john")
            .audience(audience)
            .claim("email", "john@linid.org")
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now().plusSeconds(60)))
            .build();
        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(new RSASSASigner(KEY_PAIR.getPrivate()));
        return jwt.serialize();
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
