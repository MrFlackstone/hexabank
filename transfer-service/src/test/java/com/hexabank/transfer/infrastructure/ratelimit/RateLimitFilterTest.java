package com.hexabank.transfer.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el filtro de rate limiting de forma aislada (sin contexto de Spring): la cadena de
 * producción es {@code @Profile("!test")}, por eso se prueba el filtro directamente.
 */
class RateLimitFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsUpToCapacityThenReturns429() throws Exception {
        RateLimitFilter filter = filterWithCapacity(2);
        authenticate("alice", "ROLE_USER");

        assertThat(invoke(filter).chainCalled()).isTrue();   // 1
        assertThat(invoke(filter).chainCalled()).isTrue();   // 2
        Result third = invoke(filter);                       // 3 -> excede

        assertThat(third.chainCalled()).isFalse();
        assertThat(third.response().getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(third.response().getContentType()).isEqualTo("application/problem+json");
        assertThat(third.response().getHeader("Retry-After")).isEqualTo("1");
    }

    @Test
    void staffIsExemptFromRateLimit() throws Exception {
        RateLimitFilter filter = filterWithCapacity(1);
        authenticate("staff", "ROLE_STAFF");

        for (int i = 0; i < 5; i++) {
            assertThat(invoke(filter).chainCalled()).isTrue();
        }
    }

    @Test
    void bucketsAreIndependentPerPrincipal() throws Exception {
        RateLimitFilter filter = filterWithCapacity(1);

        authenticate("alice", "ROLE_USER");
        assertThat(invoke(filter).chainCalled()).isTrue();
        assertThat(invoke(filter).chainCalled()).isFalse();  // alice agotada

        authenticate("bob", "ROLE_USER");
        assertThat(invoke(filter).chainCalled()).isTrue();    // bob tiene su propio cupo
    }

    @Test
    void disabledFilterLetsEverythingThrough() throws Exception {
        RateLimitProperties properties = properties(1);
        properties.setEnabled(false);
        RateLimitFilter filter = new RateLimitFilter(properties, new PrincipalRateLimiter(properties), objectMapper);
        authenticate("alice", "ROLE_USER");

        // shouldNotFilter() corta en OncePerRequestFilter cuando enabled=false.
        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(postTransfers(), response, chain);
            assertThat(chain.getRequest()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }
    }

    // --- helpers ---

    private RateLimitFilter filterWithCapacity(long capacity) {
        RateLimitProperties properties = properties(capacity);
        return new RateLimitFilter(properties, new PrincipalRateLimiter(properties), objectMapper);
    }

    private RateLimitProperties properties(long capacity) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setCapacity(capacity);
        properties.setRefillTokens(capacity);
        properties.setRefillPeriod(Duration.ofHours(1));  // sin recarga apreciable durante el test
        return properties;
    }

    private void authenticate(String subject, String... roles) {
        var authorities = Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(subject, "n/a", authorities));
    }

    private MockHttpServletRequest postTransfers() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/transfers");
        request.setRequestURI("/transfers");
        return request;
    }

    private Result invoke(RateLimitFilter filter) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(postTransfers(), response, chain);
        return new Result(chain.getRequest() != null, response);
    }

    private record Result(boolean chainCalled, MockHttpServletResponse response) {
    }
}
