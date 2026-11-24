package com.payflow.security;

import com.payflow.domain.Role;
import com.payflow.domain.User;
import com.payflow.web.CurrentUser;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** O "usuário do Spring Security" do PayFlow: embrulha o CurrentUser, o papel e o hash (apagado após o login). */
public final class PayflowUserDetails implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private final CurrentUser currentUser;
    private final Role role;
    private String passwordHash;

    public PayflowUserDetails(CurrentUser currentUser, Role role, String passwordHash) {
        this.currentUser = currentUser;
        this.role = role;
        this.passwordHash = passwordHash;
    }

    public static PayflowUserDetails from(User user) {
        return new PayflowUserDetails(new CurrentUser(user.getId(), user.getEmail(), user.getFullName()),
                user.getRole(), user.getPasswordHash());
    }

    public CurrentUser currentUser() {
        return currentUser;
    }

    public Role role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return currentUser.email();
    }

    /** O ProviderManager chama isto depois do login: o hash não fica na sessão. */
    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }
}
