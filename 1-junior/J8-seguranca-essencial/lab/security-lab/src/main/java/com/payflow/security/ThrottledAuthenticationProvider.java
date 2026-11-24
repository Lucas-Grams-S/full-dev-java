package com.payflow.security;

import java.util.Locale;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/** Decorador: recusa quem errou a senha vezes demais, antes de gastar um BCrypt. Vale para o formulário e para a API. */
public class ThrottledAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationProvider delegate;
    private final LoginThrottle throttle;

    public ThrottledAuthenticationProvider(AuthenticationProvider delegate, LoginThrottle throttle) {
        this.delegate = delegate;
        this.throttle = throttle;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String key = authentication.getName().trim().toLowerCase(Locale.ROOT);
        if (throttle.isLocked(key)) {
            throw new LockedException("Too many failed attempts");
        }
        try {
            Authentication result = delegate.authenticate(authentication);
            throttle.success(key);
            return result;
        } catch (BadCredentialsException e) {            // e-mail inexistente também vira BadCredentialsException (não revela quem existe)
            throttle.failure(key);
            throw e;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }
}
