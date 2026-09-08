package ds_bidding_system.gateway_service.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeyCloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt source) {
        return extractRoles(source.getClaims());
    }

    public static Collection<GrantedAuthority> extractRoles(Map<String, Object> claims) {
        Object realmAccessObj = claims.get("realm_access");

        if (!(realmAccessObj instanceof Map<?, ?> realmAccessMap)) {
            return Collections.emptySet();
        }

        Object rolesObj = realmAccessMap.get("roles");

        if (!(rolesObj instanceof List<?> roles)) {
            return Collections.emptySet();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

    }
}
