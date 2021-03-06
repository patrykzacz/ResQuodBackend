package com.ustudent.resquod.service;

import com.ustudent.resquod.exception.*;
import com.ustudent.resquod.model.User;
import com.ustudent.resquod.model.dao.*;
import com.ustudent.resquod.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Configurable(preConstruction = true, autowire = Autowire.BY_NAME)
public class UserService {


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public void validateLoginData(LoginUserData userInput) {
        if (userInput.getEmail() == null || userInput.getPassword() == null ||
                userInput.getEmail().isEmpty() || userInput.getPassword().isEmpty())
            throw new InvalidInputException();
    }

    public LoginUserData getUserDataIfExist(String email) throws EmailExistException {
        return userRepository.findUserPassword(email).orElseThrow(EmailExistException::new);
    }

    public User getLoggedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        return getUserByEmail(email);
    }

    public User getUserByEmail(String email) throws EmailExistException {
        return userRepository.findByEmail(email).orElseThrow(EmailExistException::new);

    }
    public User getUserById(Long Id) throws UserNotFoundException {
        return userRepository.findById(Id).orElseThrow(UserNotFoundException::new);
    }

    public UserData getUser(String email) throws EmailExistException {
        return userRepository.findUserData(email).orElseThrow(EmailExistException::new);
    }


    public void checkIfMailExist(String email) throws EmailExistException {
        if (userRepository.findByEmail(email).isPresent())
            throw new EmailExistException();
    }

    public void validateRegistrationData(RegisterUserData inputData) throws InvalidInputException {
        if (inputData.getEmail() == null || inputData.getEmail().length() < 2 || !inputData.getEmail().contains("@") ||
                inputData.getName() == null || inputData.getName().length() < 2 ||
                inputData.getSurname() == null || inputData.getSurname().length() < 2 ||
                inputData.getPassword() == null || inputData.getPassword().length() < 6)
            throw new InvalidInputException();
    }

    public void addUser(RegisterUserData inputData) throws RuntimeException {
        userRepository.save(new User(inputData.getName(),
                inputData.getSurname(),
                inputData.getEmail(),
                hashPassword(inputData.getPassword())));
    }

    private String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    public Boolean verifyPassword(String password, String hash) throws InvalidPasswordException {
        if (!passwordEncoder.matches(password, hash)) throw new InvalidPasswordException();
        return true;
    }

    public void updateUserData(RegisterUserData userInput) throws InvalidPasswordException, InvalidInputException {
        if (userInput.getPassword() == null || userInput.getPassword().length() < 6
                || userInput.getPassword().length() > 32)
            throw new InvalidInputException();
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);
        verifyPassword(userInput.getPassword(), user.getPassword());
        if ((userInput.getName().equals(user.getName()) && userInput.getSurname().equals(user.getSurname())
                && userInput.getEmail().equals(user.getEmail()))
                || userInput.getEmail() == null || userInput.getEmail().length() < 2
                || !userInput.getEmail().contains("@") || userInput.getPassword() == null
                || userInput.getName() == null || userInput.getName().length() < 2 ||
                userInput.getSurname() == null || userInput.getSurname().length() < 2
        )
            throw new InvalidInputException();
        user.setName(userInput.getName());
        user.setSurname(userInput.getSurname());
        user.setEmail(userInput.getEmail());
        userRepository.save(user);
    }

    public void changePassword(UserPassword userInput) throws InvalidPasswordException, PasswordMatchedException {
        if (userInput.getOldPassword() == null || userInput.getNewPassword() == null
                || userInput.getNewPassword().length() < 6 || userInput.getOldPassword().length() < 6
                || userInput.getNewPassword().length() > 32 || userInput.getOldPassword().length() > 32)
            throw new InvalidInputException();
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);
        if (userInput.getNewPassword().equals(userInput.getOldPassword())) {
            throw new PasswordMatchedException();
        }
        verifyPassword(userInput.getOldPassword(), user.getPassword());
        user.setPassword(hashPassword(userInput.getNewPassword()));
        userRepository.save(user);
    }

    public TokenTransfer login(LoginUserData userInput) {
        this.validateLoginData(userInput);
        LoginUserData loginUserData;
        loginUserData = this.getUserDataIfExist(userInput.getEmail());
        this.verifyPassword(userInput.getPassword(), loginUserData.getPassword());
        String token = jwtService.sign(loginUserData.getEmail(), loginUserData.getRole());
        UserData userData = userRepository.findUserData(loginUserData.getEmail()).orElseThrow(ObjectNotFoundException::new);
        return new TokenTransfer(token, userData);
    }
}
