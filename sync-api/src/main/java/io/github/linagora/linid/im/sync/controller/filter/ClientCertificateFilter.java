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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter accepting only the requests presenting the client certificate of linid-api.
 *
 * <p>Tomcat already rejects during the TLS handshake the clients without a certificate signed by a trusted CA. Since
 * the other components have certificates signed by the same CA, this filter also checks the subject of the client
 * certificate, and answers {@code 403} when it is not the allowed one.</p>
 */
@Slf4j
@Component
public class ClientCertificateFilter extends OncePerRequestFilter {

    /**
     * Request attribute holding the certificate chain presented by the client, starting with its own certificate.
     */
    private static final String CERTIFICATE_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";

    /**
     * Subject of the only accepted client certificate.
     */
    private final X500Principal allowedClient;

    /**
     * Creates the filter accepting only the client certificate of the given subject.
     *
     * @param allowedClient the subject of the accepted client certificate, in the RFC 2253 format
     * @throws IllegalArgumentException when the subject is blank, which would accept the certificates without subject
     */
    public ClientCertificateFilter(@Value("${sync.allowed-client}") final String allowedClient) {
        this.allowedClient = new X500Principal(Validate.notBlank(allowedClient, "sync.allowed-client must be set"));
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain)
        throws ServletException, IOException {
        X500Principal subject = subjectOf(request);

        if (!allowedClient.equals(subject)) {
            log.warn("Request rejected, client certificate subject: {}", subject);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Returns the subject of the client certificate presented with a request.
     *
     * @param request the request
     * @return the subject of the client certificate, or {@code null} when the request has none
     */
    private static X500Principal subjectOf(final HttpServletRequest request) {
        X509Certificate[] chain = (X509Certificate[]) request.getAttribute(CERTIFICATE_ATTRIBUTE);

        if (ArrayUtils.isEmpty(chain)) {
            return null;
        }

        return chain[0].getSubjectX500Principal();
    }
}
