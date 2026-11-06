package moments4j.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import moments4j.service.AppUserDetailsService;

/**
 * Port of moments/core/extensions.py (login_manager setup) + flask-login runtime.
 * All routes stay public here: access control is per-route via the decorator
 * annotations (AuthInterceptor), mirroring flask-login's @login_required style.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String REMEMBER_ME_KEY = "moments-remember-me-key";
    /** 365 days, matches flask-login's default remember cookie duration. */
    public static final long REMEMBER_ME_VALIDITY_SECONDS = 365L * 24 * 3600;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public TokenBasedRememberMeServices rememberMeServices(AppUserDetailsService userDetailsService) {
        TokenBasedRememberMeServices services = new TokenBasedRememberMeServices(REMEMBER_ME_KEY, userDetailsService);
        services.setTokenValiditySeconds((int) REMEMBER_ME_VALIDITY_SECONDS);
        services.setAlwaysRemember(false);
        services.setParameter("remember_me");
        return services;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TokenBasedRememberMeServices rememberMeServices) throws Exception {
        var csrfRepository = new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository();
        csrfRepository.setHeaderName("X-CSRFToken");  // matches main.js's X-CSRFToken header
        // plain handler: templates render the raw token in hidden inputs (flask's csrf_token() style)
        var csrfRequestHandler = new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository).csrfTokenRequestHandler(csrfRequestHandler))
                .rememberMe(remember -> remember.rememberMeServices(rememberMeServices))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());  // handled by AuthController like flask-login
        return http.build();
    }
}
