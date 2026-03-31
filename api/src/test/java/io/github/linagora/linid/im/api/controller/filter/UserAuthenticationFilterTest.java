package io.github.linagora.linid.im.api.controller.filter;

import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Test class: UserAuthenticationFilter")
class UserAuthenticationFilterTest {

    private UserAuthenticationFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new UserAuthenticationFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("test doFilterInternal: should set authentication")
    void doFilterInternal_withValidJwt_setsAuthenticationAndContinues() throws ServletException, IOException {
        // Mock Jwt
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");

        // Set Authentication with mocked Jwt as principal
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, null, List.of())
        );

        // Execute filter
        filter.doFilterInternal(request, response, filterChain);

        // Verify filterChain continued
        verify(filterChain, times(1)).doFilter(request, response);

        // Verify SecurityContext now has UserPrincipal
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getPrincipal() instanceof UserPrincipal);

        UserPrincipal user = (UserPrincipal) auth.getPrincipal();
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    @DisplayName("test doFilterInternal: should throw exception on bad authentication")
    void doFilterInternal_withNoAuthentication_throwsApiException() {
        SecurityContextHolder.getContext().setAuthentication(null);

        ApiException exception = assertThrows(ApiException.class, () ->
            filter.doFilterInternal(request, response, filterChain)
        );

        assertEquals(401, exception.getStatusCode());
        assertEquals("error.unauthorized", exception.getError().key());
    }

    @Test
    @DisplayName("test doFilterInternal: should throw exception on invalid token")
    void doFilterInternal_withInvalidPrincipal_throwsApiException() {
        // Principal is not a Jwt
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("not-a-jwt", null, List.of())
        );

        ApiException exception = assertThrows(ApiException.class, () ->
            filter.doFilterInternal(request, response, filterChain)
        );

        assertEquals(401, exception.getStatusCode());
        assertEquals("error.unauthorized", exception.getError().key());
    }
}
