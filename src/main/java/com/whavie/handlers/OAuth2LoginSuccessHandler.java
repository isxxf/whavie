package com.whavie.handlers;

import com.whavie.service.UsuarioService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UsuarioService usuarioService;

    public OAuth2LoginSuccessHandler(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String email = oauthToken.getPrincipal().getAttribute("email");
            if (email != null) {
                Long usuarioId = usuarioService.obtenerUsuarioPorEmail(email).getId();
                request.getSession().setAttribute("usuarioId", usuarioId);
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}

