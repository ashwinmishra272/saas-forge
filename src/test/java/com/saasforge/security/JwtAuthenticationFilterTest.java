package com.saasforge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void noAuthHeader_passesThrough() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void nonBearerHeader_passesThrough() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidToken_passesThrough_withoutSettingAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        when(jwtService.isTokenValid("invalid.token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validToken_setsAuthenticationInSecurityContext() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn("user@test.com");
        when(jwtService.extractRole(token)).thenReturn("ADMIN");
        when(jwtService.extractTenantId(token)).thenReturn(1L);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo("user@test.com");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    void validToken_setsTenantContextDuringFilter() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn("user@test.com");
        when(jwtService.extractRole(token)).thenReturn("ADMIN");
        when(jwtService.extractTenantId(token)).thenReturn(42L);

        Long[] capturedTenantId = {null};
        doAnswer(invocation -> {
            capturedTenantId[0] = TenantContext.getCurrentTenantId();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedTenantId[0]).isEqualTo(42L);
    }

    @Test
    void validToken_clearsTenantContextAfterFilter() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn("user@test.com");
        when(jwtService.extractRole(token)).thenReturn("ADMIN");
        when(jwtService.extractTenantId(token)).thenReturn(99L);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }

    @Test
    void validToken_callsFilterChain() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn("user@test.com");
        when(jwtService.extractRole(token)).thenReturn("ADMIN");
        when(jwtService.extractTenantId(token)).thenReturn(1L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}