package com.payflow.security;

import com.payflow.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Ensina o Spring Security a achar um usuário pelo "username" (no PayFlow, o e-mail). */
@Service
public class PayflowUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public PayflowUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return users.findByEmail(normalized)
                .map(PayflowUserDetails::from)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));    // sem o e-mail na mensagem (vai para log)
    }
}
