package com.ustudent.resquod.controller;

import com.ustudent.resquod.exception.EmailExistException;
import com.ustudent.resquod.exception.InvalidInputException;
import com.ustudent.resquod.exception.InvalidPasswordException;
import com.ustudent.resquod.exception.PasswordMatchedException;
import com.ustudent.resquod.model.dao.*;

import com.ustudent.resquod.service.JwtService;
import com.ustudent.resquod.service.UserService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Api(value = "User management")
public class AuthorizationController {

    private final JwtService jwtService;
    private final UserService userService;

    @Autowired
    public AuthorizationController(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @ApiOperation(value = "Create new user")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully created!"),
            @ApiResponse(code = 400, message = "\"Invalid input!\" or \"Email already taken!\""),
            @ApiResponse(code = 500, message = "User cannot be registered!")})
    @PostMapping(value = "/register")
    public ResponseTransfer register(
            @ApiParam(value = "Required email, name, surname, password", required = true)
            @RequestBody RegisterUserData inputData) {
        try {
            userService.checkIfMailExist(inputData.getEmail());
            userService.validateRegistrationData(inputData);
            userService.addUser(inputData);
        } catch (EmailExistException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already taken!", ex);
        } catch (InvalidInputException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input!", ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User cannot be registered!");
        }
        return new ResponseTransfer("Successfully created!");

    }

    @ApiOperation(value = "Login as user")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "{\n" +
            "    \"token\": \"string\"\n" +
            "}"),
            @ApiResponse(code = 400, message = "\"Invalid input!\" or \"Email don't exist!\" or \"Invalid password!\""),
            @ApiResponse(code = 500, message = "Server Error!")})
    @PostMapping("/login")
    public TokenTransfer login(
            @ApiParam(value = "Required email, password", required = true)
            @RequestBody LoginUserData userInput) {
        try {
            return userService.login(userInput);
        } catch (InvalidInputException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input!");
        } catch (EmailExistException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email don't exist!");
        } catch (InvalidPasswordException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password!");
        }

    }

    @ApiOperation(value = "Get current user", authorizations = {@Authorization(value = "authkey")})
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 500, message = "Server Error!")})
    @GetMapping("/user")
    public UserData getUser() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
            return userService.getUser(email);
        } catch (EmailExistException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request");
        }
    }

    @ApiOperation(value = "Change user data", authorizations = {@Authorization(value = "authkey")})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully updated!"),
            @ApiResponse(code = 400, message = "\"Invalid input!\" or \"Invalid password!\""),
            @ApiResponse(code = 500, message = "Server Error!")})
    @PatchMapping("userPatch")
    public ResponseTransfer changeUserData(
            @ApiParam(value = "Required email, name, surname, password", required = true)
            @RequestBody RegisterUserData userInput) {
        try {
            userService.updateUserData(userInput);
        } catch (InvalidInputException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input!");
        } catch (InvalidPasswordException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password!");
        }
        return new ResponseTransfer("Successfully updated!");
    }

    @ApiOperation(value = "password", authorizations = {@Authorization(value = "authkey")})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully changed!"),
            @ApiResponse(code = 400, message = "\"Password too short or too long\" or \"Password don't match!\" or \"Password Canno't be the same!\""),
            @ApiResponse(code = 500, message = "Password Cannot be changed")})
    @PatchMapping(value = "/password")
    public ResponseTransfer changePassowrd(
            @ApiParam(value = "Required oldPassword, newPassword", required = true)
            @RequestBody UserPassword inputData) {
        try {
            userService.changePassword(inputData);
        } catch (InvalidInputException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password too short or too long!", ex); }
         catch (InvalidPasswordException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password don't match!", ex);
        } catch (PasswordMatchedException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password Canno't be the same!", ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Password Canno't be changed!", ex);
        }

        return new ResponseTransfer("Successfully changed!");

    }

}
