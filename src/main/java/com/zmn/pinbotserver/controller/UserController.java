package com.zmn.pinbotserver.controller;


import com.zmn.pinbotserver.model.jwt.JwtRequest;
import com.zmn.pinbotserver.model.jwt.JwtResponse;
import com.zmn.pinbotserver.model.user.User;
import com.zmn.pinbotserver.security.JwtUtil;
import com.zmn.pinbotserver.service.user.UserDetailsServiceImpl;
import com.zmn.pinbotserver.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/authenticate")
    public JwtResponse createAuthenticationToken(@RequestBody JwtRequest authenticationRequest) throws Exception {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
        );

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);

        return new JwtResponse(jwt);
    }

    @PostMapping("/register")
    public String registerUser(@RequestBody User user) throws Exception {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.add(user);
        return "User registered successfully";
    }
}