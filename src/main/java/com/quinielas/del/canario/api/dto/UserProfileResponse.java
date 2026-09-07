package com.quinielas.del.canario.api.dto;

public class UserProfileResponse {

    private Long id;
    private String username;
    private String email;
    private String role;

    public UserProfileResponse() {}

    public UserProfileResponse(Long id, String username, String email, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public Long getId()                { return id; }
    public void setId(Long id)         { this.id = id; }

    public String getUsername()        { return username; }
    public void setUsername(String u)  { this.username = u; }

    public String getEmail()           { return email; }
    public void setEmail(String e)     { this.email = e; }

    public String getRole()            { return role; }
    public void setRole(String r)      { this.role = r; }
}

