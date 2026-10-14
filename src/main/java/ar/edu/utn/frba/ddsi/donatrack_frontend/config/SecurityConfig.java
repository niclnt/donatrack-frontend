package ar.edu.utn.frba.ddsi.donatrack_frontend.config;

import ar.edu.utn.frba.ddsi.donatrack_frontend.providers.CustomAuthProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomAuthProvider customAuthProvider) throws Exception {
        http
                .authenticationProvider(customAuthProvider)
                .authorizeHttpRequests(auth -> auth
                        // Recursos estaticos y paginas publicas
                        .requestMatchers("/", "/login", "/registro", "/registro/**","/legales", "/privacidad", "/mapa-donaciones", "/403", "/404", "/error", "/css/**", "/js/**", "/img/**", "/assets/**").permitAll()
                        .requestMatchers("/admin/**").hasRole(SesionUsuario.ROL_ADMIN)
                        .requestMatchers("/donante/**").hasAnyRole(SesionUsuario.ROL_DONANTE_HUMANO, SesionUsuario.ROL_DONANTE_JURIDICO)
                        .requestMatchers("/ong/**").hasRole(SesionUsuario.ROL_BENEFICIARIO)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(redireccionPorRol())
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        // No autenticado -> login
                        .authenticationEntryPoint((request, response, e) -> response.sendRedirect("/login?unauthorized"))
                        // Autenticado pero sin permisos -> 403
                        .accessDeniedHandler((request, response, e) -> response.sendRedirect("/403"))
                );

        return http.build();
    }

    // Despues del login, cada rol va a su panel
    private AuthenticationSuccessHandler redireccionPorRol() {
        return (request, response, authentication) -> {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring("ROLE_".length()))
                    .toList();
            response.sendRedirect(SesionUsuario.panelPara(roles));
        };
    }
}
