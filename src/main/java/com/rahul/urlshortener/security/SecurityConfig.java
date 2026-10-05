package com.rahul.urlshortener.security;

import com.rahul.urlshortener.config.AppProperties;
import com.rahul.urlshortener.filter.RateLimitFilter;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final AppProperties props;
    private final StringRedisTemplate redis;

    public SecurityConfig(AppProperties props, StringRedisTemplate redis) {
        this.props = props;
        this.redis = redis;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        RateLimitFilter rateLimit = new RateLimitFilter(redis, props);
        ApiKeyFilter apiKey = new ApiKeyFilter(props.security() == null ? null : props.security().apiKey());
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**",
                                "/actuator/info", "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/analytics/*", "/*").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> writeProblem(res, 401,
                        "Unauthorized", "Missing or invalid API key")))
                .addFilterBefore(rateLimit, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKey, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void writeProblem(jakarta.servlet.http.HttpServletResponse res, int status, String title,
                                     String detail) throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json");
        res.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
                + ",\"detail\":\"" + detail + "\"}");
    }
}
