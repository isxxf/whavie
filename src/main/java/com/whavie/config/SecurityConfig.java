package com.whavie.config;

import com.whavie.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${remember.me.key}")
    private String rememberMeKey;

    @Value("${app.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService,
            UserDetailsService userDetailsService,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/favicon.ico", "/login", "/registro", "/reset-password", "/css/**", "/js/**", "/img/**",
                                "/unirse", "/ws/**", "/topic/**", "/app/**", "/votacion", "/votacion/**", "/resultado/**",
                                "/api/auth/**", "/ping"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/sala/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/sala/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/sala/*/votar").permitAll()
                        .requestMatchers(HttpMethod.POST, "/sala/*/salir").permitAll()
                        .requestMatchers("/sala/**").authenticated()
                        .requestMatchers("/descubrir").authenticated()
                        .requestMatchers("/api/preferencias/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                )
                .rememberMe(rememberMe -> rememberMe
                        .key(rememberMeKey)
                        .rememberMeParameter("remember-me")
                        .userDetailsService(userDetailsService)
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oauth2UserService)
                        )
                        .defaultSuccessUrl("/", true)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/ws/**", "/app/**", "/topic/**")
                        .ignoringRequestMatchers("/api/auth/**")
                )
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(contentTypeOptions -> {
                        })
                        .xssProtection(xssProtection -> {
                        })
                        .cacheControl(cacheControl -> {
                        })
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return usernameOrEmail -> {
            String normalized = usernameOrEmail.trim().toLowerCase();
            return usuarioRepository
                    .findByEmailOrUsername(normalized, normalized)
                    .map(usuario -> User.withUsername(usuario.getUsername())
                            .password(usuario.getPassword() != null ? usuario.getPassword() : "")
                            .roles("USER")
                            .build()
                    )
                    .orElseThrow(() ->
                            new UsernameNotFoundException("Usuario no encontrado: " + usernameOrEmail));
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .toList()
        );
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
