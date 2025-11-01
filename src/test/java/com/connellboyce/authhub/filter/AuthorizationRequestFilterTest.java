package com.connellboyce.authhub.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.mockito.Mockito.*;

class AuthorizationRequestFilterTest {
    private HttpServletRequest request;
    private ServletResponse response;
    private FilterChain filterChain;
    private HttpSession session;
    private AuthorizationRequestFilter filter;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(ServletResponse.class);
        filterChain = mock(FilterChain.class);
        session = mock(HttpSession.class);

        when(request.getSession()).thenReturn(session);

        filter = new AuthorizationRequestFilter(Set.of("client_id", "redirect_uri"));
    }

    @Test
    void shouldPreserveSpecifiedParametersInSession() throws IOException, ServletException {
        when(request.getParameter("client_id")).thenReturn("my-client");
        when(request.getParameter("redirect_uri")).thenReturn("https://example.com/callback");

        filter.doFilter(request, response, filterChain);

        verify(session).setAttribute("auth_param_client_id", "my-client");
        verify(session).setAttribute("auth_param_redirect_uri", "https://example.com/callback");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipNullParameters() throws IOException, ServletException {
        when(request.getParameter("client_id")).thenReturn(null);
        when(request.getParameter("redirect_uri")).thenReturn("https://example.com/callback");

        filter.doFilter(request, response, filterChain);

        verify(session, never()).setAttribute(eq("auth_param_client_id"), any());
        verify(session).setAttribute("auth_param_redirect_uri", "https://example.com/callback");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAlwaysInvokeFilterChain() throws IOException, ServletException {
        filter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldHandleEmptyParameterSetGracefully() throws IOException, ServletException {
        AuthorizationRequestFilter emptyFilter = new AuthorizationRequestFilter(Set.of());
        emptyFilter.doFilter(request, response, filterChain);

        verify(session, never()).setAttribute(anyString(), any());
        verify(filterChain).doFilter(request, response);
    }

}
