package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.usf.inspect.server.dao.AdminDao;
import org.usf.inspect.server.exception.InvalidNamespaceException;
import org.usf.inspect.server.exception.NamespaceAlreadyExistsException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@RequiredArgsConstructor
@Service
public class AdminService {
    private final AdminDao adminDao;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public boolean addNamespace(String namespace, String token) {
        if (isNull(namespace) || namespace.isBlank() || !namespace.matches("[a-zA-Z0-9_\\-]+")) {
            throw new InvalidNamespaceException("Namespace invalide: " + namespace);
        }
        var encryptedToken = nonNull(token) ? passwordEncoder.encode(token) : null;
        try {
            adminDao.saveNamespace(namespace, encryptedToken);
        } catch (DataIntegrityViolationException e) {
            throw new NamespaceAlreadyExistsException("Namespace déjà existant: " + namespace);
        }
        return true;
    }

}
