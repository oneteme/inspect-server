package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.server.dto.NamespaceDto;
import org.usf.inspect.server.service.AdminService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin()
@RequestMapping(value = "/admin", produces = APPLICATION_JSON_VALUE)
public class AdminController {
    private final AdminService adminService;

    @PostMapping("namespace/{namespace}")
    public ResponseEntity<Boolean> addNamespace(@PathVariable String namespace, @RequestBody NamespaceDto nsp) {
        return ResponseEntity.ok(adminService.addNamespace(namespace, nsp.password()));
    }
}
