package com.voicememo.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicememo.config.RateLimitConfig;
import com.voicememo.dto.ErrorResponse;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String ip = getClientIp(httpReq);
        String path = httpReq.getRequestURI();

        // Use tighter bucket for upload endpoint
        Bucket bucket = path.contains("/upload")
                ? rateLimitConfig.resolveUploadBucket(ip)
                : rateLimitConfig.resolveBucket(ip);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add remaining requests header for client visibility
            httpResp.addHeader("X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            httpResp.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResp.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResp.addHeader("X-Rate-Limit-Retry-After-Seconds",
                    String.valueOf(waitSeconds));

            ErrorResponse error = ErrorResponse.of(
                    429,
                    "Too Many Requests",
                    "Rate limit exceeded. Try again in " + waitSeconds + "s",
                    path);

            httpResp.getWriter().write(objectMapper.writeValueAsString(error));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}