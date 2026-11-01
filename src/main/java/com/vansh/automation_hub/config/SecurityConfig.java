package com.vansh.automation_hub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Health check sabke liye khula (AWS ko chahiye)
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // TWEAK 1: Website ki files (HTML/CSS/JS) khuli. Inme koi secret nahi hota.
                        // Login page sabko dikhega, par DATA (/api/**) bina login ke nahi milega.
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                        // Baaki SAB kuch (yaani poori /api/**) login ke peeche
                        .anyRequest().authenticated())

                // TWEAK 2: Galat/missing login par sirf 401 do, "WWW-Authenticate" header ke bina.
                // Isi header ki wajah se browser apna purana login popup dikhata tha.
                // Ab hamara apna login form kaam karega. Postman par koi farak nahi padta.
                .httpBasic(basic -> basic
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                // Content Security Policy: browser ko batata hai ki page kahan se kya load kar sakta hai.
                // Agar kisi ne kabhi script inject bhi kar di, toh browser use chalne nahi dega.
                // (sessionStorage mein credentials hain, isliye ye extra suraksha zaroori hai)
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; "
                                + "script-src 'self'; "
                                + "style-src 'self' https://fonts.googleapis.com; "
                                + "font-src https://fonts.gstatic.com; "
                                + "img-src 'self' data:; "
                                + "connect-src 'self'; "
                                + "base-uri 'self'; "
                                + "form-action 'self'; "
                                + "frame-ancestors 'none'")));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder,
                                                 @Value("${app.security.username}") String username,
                                                 @Value("${app.security.password}") String password) {
        if (username.isBlank() || password.length() < 12) {
            throw new IllegalStateException(
                    "APP_USERNAME must not be blank and APP_PASSWORD must be at least 12 characters");
        }

        UserDetails admin = User.withUsername(username)
                .password(encoder.encode(password))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
}