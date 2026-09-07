package com.quinielas.del.canario.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdatePerfilRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 80, message = "El apellido paterno no puede superar 80 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(max = 80, message = "El apellido materno no puede superar 80 caracteres")
    private String apellidoMaterno;

    @NotBlank(message = "La ciudad es obligatoria")
    @Size(max = 100, message = "La ciudad no puede superar 100 caracteres")
    private String ciudad;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    private String telefono;

    // ─── Getters / Setters ────────────────────────────────────────────
    public String getNombre()                        { return nombre; }
    public void setNombre(String nombre)             { this.nombre = nombre; }

    public String getApellidoPaterno()               { return apellidoPaterno; }
    public void setApellidoPaterno(String ap)        { this.apellidoPaterno = ap; }

    public String getApellidoMaterno()               { return apellidoMaterno; }
    public void setApellidoMaterno(String am)        { this.apellidoMaterno = am; }

    public String getCiudad()                        { return ciudad; }
    public void setCiudad(String ciudad)             { this.ciudad = ciudad; }

    public LocalDate getFechaNacimiento()            { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fn)     { this.fechaNacimiento = fn; }

    public String getTelefono()                      { return telefono; }
    public void setTelefono(String telefono)         { this.telefono = telefono; }
}

