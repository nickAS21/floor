package org.nickas21.smart.security.controller;


import lombok.Data;
import org.nickas21.smart.util.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AuthRequest {
    @NotBlank(message = "Name cannot be blank")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 4, max = 20, message = "Password must be between 6 and 20 characters")
    private String password;

}
