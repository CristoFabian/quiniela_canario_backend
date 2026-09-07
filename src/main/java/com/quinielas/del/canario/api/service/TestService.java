package com.quinielas.del.canario.api.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TestService {

    public Map<String, String> getPrivado(UserDetails user) {
        return Map.of(
                "mensaje", "Acceso autorizado",
                "usuario", user.getUsername(),
                "rol",     user.getAuthorities().iterator().next().getAuthority()
        );
    }

    public Map<String, String> getPublico() {
        return Map.of("mensaje", "Endpoint público, sin token necesario");
    }

    public String getHealth() {
        return "API OK";
    }
}

