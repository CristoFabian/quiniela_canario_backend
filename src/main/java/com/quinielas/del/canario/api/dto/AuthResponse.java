package com.quinielas.del.canario.api.dto;

public class AuthResponse {

    private String token;
    private String username;
    private String role;

    // ─── Constructores ────────────────────────────────────────────────
    public AuthResponse() {}

    public AuthResponse(String token, String username, String role) {
        this.token = token;
        this.username = username;
        this.role = role;
    }

    // ─── Builder ──────────────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String token;
        private String username;
        private String role;

        public Builder token(String t)    { this.token = t;    return this; }
        public Builder username(String u) { this.username = u; return this; }
        public Builder role(String r)     { this.role = r;     return this; }

        public AuthResponse build() { return new AuthResponse(token, username, role); }
    }

    // ─── Getters / Setters ────────────────────────────────────────────
    public String getToken()              { return token; }
    public void setToken(String token)    { this.token = token; }

    public String getUsername()           { return username; }
    public void setUsername(String u)     { this.username = u; }

    public String getRole()               { return role; }
    public void setRole(String role)      { this.role = role; }
}
