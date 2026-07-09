package com.hexabank.transfer.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate limiting por principal en {@code POST /transfers} (Fase 11, defensa en profundidad). Se ubica
 * tras la autenticación en la cadena de filtros, por lo que el principal ({@code sub}) ya está
 * resuelto. STAFF queda exento (operativa interna). Al agotarse el cupo responde {@code 429} con un
 * {@link ProblemDetail} (RFC 7807), coherente con el resto de errores del servicio.
 *
 * <p>Se escribe la respuesta directamente: al ejecutarse antes del {@code DispatcherServlet}, el
 * {@code @RestControllerAdvice} no puede interceptar una excepción lanzada aquí.</p>
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String STAFF_AUTHORITY = "ROLE_STAFF";
    private static final String TRANSFERS_PATH = "/transfers";

    private final RateLimitProperties properties;
    private final PrincipalRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitProperties properties,
                           PrincipalRateLimiter rateLimiter,
                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled()
                || !HttpMethod.POST.matches(request.getMethod())
                || !TRANSFERS_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Sin principal (no debería ocurrir tras la autenticación) o STAFF -> sin límite.
        if (authentication == null || isStaff(authentication) || rateLimiter.tryConsume(authentication.getName())) {
            filterChain.doFilter(request, response);
            return;
        }

        writeTooManyRequests(response);
    }

    private boolean isStaff(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> STAFF_AUTHORITY.equals(authority.getAuthority()));
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Límite de peticiones excedido. Reduce la frecuencia de transferencias.");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, "1");
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
