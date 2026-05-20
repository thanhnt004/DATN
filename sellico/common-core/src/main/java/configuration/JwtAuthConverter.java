package configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JwtAuthConverter implements Converter<Jwt, JwtAuthenticationToken> {
    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Value("${jwt.auth.converter.principle-attribute:preferred_username}")
    private String principleAttribute;

    @Value("${jwt.auth.converter.resource-id:}")
    private String resourceId;

    @Override
    public JwtAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                Optional.ofNullable(jwtGrantedAuthoritiesConverter.convert(jwt))
                        .map(Collection::stream)
                        .orElseGet(Stream::empty),
                extractRoles(jwt).stream()
        ).collect(Collectors.toSet());

        System.out.println("JWT Authorities: " + authorities);
        System.out.println("JWT Claims: " + jwt.getClaims());

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                getPrincipleClaimName(jwt)
        );
    }

    private String getPrincipleClaimName(Jwt jwt) {
        String claimName = JwtClaimNames.SUB;
        if (principleAttribute != null) {
            claimName = principleAttribute;
        }
        return jwt.getClaimAsString(claimName);
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        Stream<GrantedAuthority> resourceRolesStream = Stream.empty();
        Stream<GrantedAuthority> realmRolesStream = Stream.empty();

        if (resourceAccess != null && resourceId != null && resourceAccess.containsKey(resourceId)) {
            Map<String, Object> resource = (Map<String, Object>) resourceAccess.get(resourceId);
            if (resource != null && resource.containsKey("roles")) {
                Collection<String> roles = (Collection<String>) resource.get("roles");
                if (roles != null) {
                    resourceRolesStream = roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role));
                }
            }
        }

        if (realmAccess != null && realmAccess.containsKey("roles")) {
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            if (roles != null) {
                realmRolesStream = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role));
            }
        }

        return Stream.concat(resourceRolesStream, realmRolesStream).collect(Collectors.toSet());
    }
}
