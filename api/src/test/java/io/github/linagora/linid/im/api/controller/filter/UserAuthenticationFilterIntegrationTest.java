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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.linagora.linid.im.api.config.SecurityConfig;
import io.github.linagora.linid.im.api.controller.UserPreferenceController;
import io.github.linagora.linid.im.api.model.user.preference.UserPreferenceMapper;
import io.github.linagora.linid.im.api.persistence.model.Account;
import io.github.linagora.linid.im.api.service.AccountService;
import io.github.linagora.linid.im.api.service.UserPreferenceService;
import io.github.linagora.linid.im.corelib.i18n.I18nService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserPreferenceController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "AUTH_ISSUER_URI=https://auth.test",
    "AUTH_JWK_SET_URI=https://auth.test/oauth2/jwks"
})
@DisplayName("Test class: UserAuthenticationFilter (integration)")
class UserAuthenticationFilterIntegrationTest {

    private static final String EMAIL = "john@linid.org";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private UserPreferenceService userPreferenceService;

    @MockitoBean
    private UserPreferenceMapper userPreferenceMapper;

    @MockitoBean
    private I18nService i18nService;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("john").claim("email", EMAIL).build();
        Mockito.when(jwtDecoder.decode("token")).thenReturn(jwt);
        Mockito.when(userPreferenceService.findAll(Mockito.any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("test doFilterInternal: should serve the request when an account matches the token")
    void testAcceptsTokenWithAccount() throws Exception {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        Mockito.when(accountService.getAccountByEmail(EMAIL)).thenReturn(Optional.of(account));

        mockMvc.perform(get("/user-preferences").header("Authorization", "Bearer token"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("test doFilterInternal: should respond 401 when no account matches the token")
    void testRejectsTokenWithoutAccount() throws Exception {
        Mockito.when(accountService.getAccountByEmail(EMAIL)).thenReturn(Optional.empty());

        mockMvc.perform(get("/user-preferences").header("Authorization", "Bearer token"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate", Matchers.startsWith("Bearer ")));
    }
}
