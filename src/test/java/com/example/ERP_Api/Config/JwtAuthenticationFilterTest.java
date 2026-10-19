package com.example.ERP_Api.Config;


import com.example.ERP_Api.Entity.Role;
import com.example.ERP_Api.Service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = new JwtService();
        filter = new JwtAuthenticationFilter(jwtService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void runFilterWithToken(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
    }

    @Test
    void authenticatesValidBearerToken() throws Exception {
        runFilterWithToken(jwtService.generateToken("user@example.com", Role.USER));

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user@example.com", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void grantsRoleUserForUserToken() throws Exception {
        runFilterWithToken(jwtService.generateToken("user@example.com", Role.USER));

        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(authorities.stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void grantsRoleAdminForAdminToken() throws Exception {
        runFilterWithToken(jwtService.generateToken("admin@example.com", Role.ADMIN));

        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void leavesMalformedBearerTokenUnauthenticated() throws Exception {
        runFilterWithToken("not-a-jwt");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void leavesRequestWithoutAuthorizationHeaderUnauthenticated() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> {});

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}