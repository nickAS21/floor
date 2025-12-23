package org.nickas21.smart.data.controller;

import org.nickas21.smart.data.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;

public abstract class BaseController {

    @Autowired
    protected UserService userService;

    @ModelAttribute
    protected void authorize(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        if (!userService.validateToken(token)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token"
            );
        }
    }
}