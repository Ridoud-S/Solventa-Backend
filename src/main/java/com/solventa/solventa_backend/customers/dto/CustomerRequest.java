package com.solventa.solventa_backend.customers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 150)
    private String name;

    @Size(max = 200)
    private String company;

    @Email(message = "Correo inválido")
    @Size(max = 200)
    private String email;

    @Size(max = 30)
    private String phone;

    @Size(max = 13)
    private String rfc;

    private String address;

    private String notes;

    private String assignedToId;
}