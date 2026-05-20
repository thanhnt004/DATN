package org.example.identityservice.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CookieUtils {
    @Value("${app.cookie.domain:localhost}")
    private String cookieDomain;

    @Value("${app.cookie.secure:false}")
    private boolean isSecure;

    @Value("${app.cookie.refresh-token-expiration:604800}")
    private long maxAgeSeconds;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    public Cookie createCookie(String value, String name) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setPath("/");
        cookie.setMaxAge((int) maxAgeSeconds);
        if (!"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }
        cookie.setAttribute("SameSite", sameSite);
        return cookie;
    }

    public void clearCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        if (!"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }
        cookie.setAttribute("SameSite", sameSite);
        response.addCookie(cookie);
    }

    public Optional<Cookie> readCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null)
            return Optional.empty();
        for (Cookie c : req.getCookies()) {
            if (name.equals(c.getName()))
                return Optional.of(c);
        }
        return Optional.empty();
    }
}

