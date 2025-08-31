package org.yaroslaavl.cvservice.config.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KeyCloakAuthenticationRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLE_CLAIM = "recruiter_app_roles";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection <GrantedAuthority> grantedAuthorities = new ArrayList<>();
        List<String> access_roles = jwt.getClaim(ROLE_CLAIM);

        if (access_roles != null && !access_roles.isEmpty()) {
            List<String> filteredRoles = access_roles.stream()
                    .filter(role -> role.startsWith("ROLE_")).toList();

            if (!filteredRoles.isEmpty()) {
                filteredRoles.forEach(role -> grantedAuthorities.add(new SimpleGrantedAuthority(role)));
            }
        }
        return grantedAuthorities;
    }
}
