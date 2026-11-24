package com.payflow.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Do token para as autoridades do Spring: o claim "scope" vira SCOPE_x (o conversor padrão) e
 * realm_access.roles vira ROLE_x. Escopo é o que o CLIENTE foi autorizado a fazer;
 * papel é o que o USUÁRIO pode fazer: as duas perguntas são diferentes e as duas valem.
 */
public class KeycloakAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
        if (jwt.getClaim("realm_access") instanceof Map<?, ?> realm && realm.get("roles") instanceof Collection<?> roles) {
            for (Object role : roles) {
                if (role instanceof String name) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + name));
                }
            }
        }
        return authorities;
    }
}
