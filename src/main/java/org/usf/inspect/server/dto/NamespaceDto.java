package org.usf.inspect.server.dto;

import jakarta.validation.constraints.NotBlank;

public record NamespaceDto(@NotBlank String password) {
}
