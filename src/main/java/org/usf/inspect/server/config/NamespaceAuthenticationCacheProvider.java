package org.usf.inspect.server.config;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated;
import static org.usf.inspect.core.Assertions.assertIdentifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
//@ConditionalOnProperty(name = "spring.security.enabled", havingValue = "true")
public class NamespaceAuthenticationCacheProvider implements AuthenticationProvider {

    private final JdbcTemplate template;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final Map<String, String> cache = new ConcurrentHashMap<String, String>();
	private final Map<String, Object> locks = new ConcurrentHashMap<String, Object>();

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        var namespace  = authentication.getName();
        var token = nonNull(authentication.getCredentials()) ? authentication.getCredentials().toString() : null;
        synchronized (lockFor(namespace)) {
            var expected = cache.computeIfAbsent(namespace, k->{
            	try {
            		return getEncryptedToken(namespace);
            	}
            	catch (EmptyResultDataAccessException e) {
                    return saveNamespace(namespace, token);
            	}
            });
            if ((isNull(token) && nonNull(expected)) || !passwordEncoder.matches(token, expected)) {
            	throw new BadCredentialsException("Invalid token for namespace: " + namespace);
            }
            return authenticated(namespace, null, emptyList());
		}
    }
    
    public Object lockFor(String namespace){
    	return locks.computeIfAbsent(namespace, v->{
    		if(locks.size() <= 20) {
    			return new Object();
    		}
    		throw new IllegalStateException("too many namespaces");
    	});
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public String getEncryptedToken(String namespace) {
    	assertIdentifier(namespace, "namespace");
        return template.queryForObject("SELECT va_nam, va_enc_tkn FROM e_nsp_ins WHERE va_nam=?",
            (rs, idx)-> rs.getString("va_enc_tkn"), namespace);
    }

    public String saveNamespace(String namespace, String token) {
    	assertIdentifier(namespace, "namespace");
		var encryptedToken = nonNull(token) ? passwordEncoder.encode(token) : null;
        template.update("INSERT INTO e_nsp_ins(va_nam, va_enc_tkn) VALUES(?, ?)", namespace, encryptedToken);
        return encryptedToken;
    }
}
