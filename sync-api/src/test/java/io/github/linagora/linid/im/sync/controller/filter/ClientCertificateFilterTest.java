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

package io.github.linagora.linid.im.sync.controller.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("Test class: ClientCertificateFilter")
class ClientCertificateFilterTest {

    private static final String CERTIFICATE_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";

    private final ClientCertificateFilter filter = new ClientCertificateFilter("CN=linid-api");

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    @DisplayName("doFilter should let through a request with the client certificate of linid-api")
    void testDoFilter_allowedCertificate() throws ServletException, IOException {
        request.setAttribute(CERTIFICATE_ATTRIBUTE, new X509Certificate[] {certificate("CN=linid-api")});

        filter.doFilter(request, response, filterChain);

        assertSame(request, filterChain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("doFilter should reject with 403 a request with the client certificate of another component")
    void testDoFilter_otherCertificate() throws ServletException, IOException {
        request.setAttribute(CERTIFICATE_ATTRIBUTE, new X509Certificate[] {certificate("CN=linid.localtest.me")});

        filter.doFilter(request, response, filterChain);

        assertNull(filterChain.getRequest());
        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("doFilter should reject with 403 a request without client certificate")
    void testDoFilter_noCertificate() throws ServletException, IOException {
        filter.doFilter(request, response, filterChain);

        assertNull(filterChain.getRequest());
        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("constructor should reject a blank allowed client, which would accept the certificates without subject")
    void testConstructor_blankAllowedClient() {
        assertThrows(IllegalArgumentException.class, () -> new ClientCertificateFilter(" "));
    }

    private static X509Certificate certificate(final String subject) {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getSubjectX500Principal()).thenReturn(new X500Principal(subject));
        return certificate;
    }
}
