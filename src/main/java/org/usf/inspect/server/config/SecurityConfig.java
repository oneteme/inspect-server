package org.usf.inspect.server.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "spring.security.enabled", havingValue = "true")
public class SecurityConfig {
	
    @Bean
    @Order(1)
    public SecurityFilterChain basicSecurityFilterChain(HttpSecurity http, NamespaceAuthenticationCacheProvider provider) throws Exception {
    	return http
    			.csrf(AbstractHttpConfigurer::disable)
    			.securityMatcher("/v5/trace/**")
    			.authenticationProvider(provider)
    			.httpBasic(withDefaults())
    			.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
    			.build();
    }
    
	@Bean
    @Order(2)
	SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin)) //H2 frames
				.oauth2ResourceServer(oauth2 -> oauth2.jwt())
				.authorizeHttpRequests(auth-> auth
						.requestMatchers("/public/**", "/h2/**", "/actuator/**", "/v4/trace/**").permitAll()
						.anyRequest().authenticated())
				.build();
	}
}