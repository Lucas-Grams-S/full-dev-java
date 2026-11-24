package com.payflow.web.api;

import com.payflow.security.PayflowUserDetails;
import com.payflow.security.TokenService;
import com.payflow.web.api.dto.LoginRequest;
import com.payflow.web.api.dto.TokenResponse;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Troca e-mail e senha por um access token (JWT). A senha só passa por aqui e pelo formulário do painel. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokens;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokens) {
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
    }

    @PostMapping("/token")
    public TokenResponse token(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim().toLowerCase(Locale.ROOT), request.password()));
        var issued = tokens.issue((PayflowUserDetails) authentication.getPrincipal());
        return new TokenResponse(issued.value(), "Bearer", tokens.ttl().toSeconds());
    }
}
