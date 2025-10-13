package za.ac.cput.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, UserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Disable default form login and httpBasic; Security will use JWT filter and our UserDetailsService for auth
        http
            .httpBasic().disable()
            .formLogin().disable();

        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .authorizeHttpRequests(auth -> auth
                // public endpoints: allow registration, login, book list and book images (so browsers can fetch <img src>)
                .requestMatchers(
                    "/api/users/create",
                    "/api/users/login",
                    "/api/book/all",
                    "/api/book/*/image",
                    "/api/book/*/image-info",
                    // allow public contact/newsletter submissions
                    "/api/contacts",
                    "/api/contacts/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Build an AuthenticationManager that uses our UserDetailsService + password encoder
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        // PasswordEncoder bean will be picked up automatically by DaoAuthenticationProvider (from context)

        AuthenticationManager authenticationManager = new org.springframework.security.authentication.ProviderManager(provider);
        http.setSharedObject(AuthenticationManager.class, authenticationManager);

        return http.build();
    }
}
