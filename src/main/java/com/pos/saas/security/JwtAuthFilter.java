package com.pos.saas.security;

import com.pos.saas.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtProvider.isValid(token)) {
                    Claims claims = jwtProvider.parse(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
                    // User Account Disabled থাকলে Authentication না করে, পরবর্তী Filter-এ Request পাঠিয়ে দেওয়া
                    if (!userDetails.isEnabled()) {
                        chain.doFilter(request, response);
                        return;
                    }

                    // User-এর Authentication Object তৈরি করা, এখানে User-এর পরিচয় এবং Role/Authority সংরক্ষণ করা হয়
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    // বর্তমান Request-এর অতিরিক্ত তথ্য Authentication-এ যুক্ত করা
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // Spring Security Context-এ User-এর Authentication সংরক্ষণ করা
                    // ফলে পরবর্তী Controller ও Service থেকে User-এর পরিচয় পাওয়া যায়
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    Object tenant = claims.get("tenantId");
                    if (tenant instanceof Number n) {
                        TenantContext.setTenantId(n.longValue());
                    }
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
