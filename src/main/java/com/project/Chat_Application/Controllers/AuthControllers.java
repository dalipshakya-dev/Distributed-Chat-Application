package com.project.Chat_Application.Controllers;




import com.project.Chat_Application.Dto.SecurityDto.Request.LoginRequestDto;
import com.project.Chat_Application.Dto.SecurityDto.Request.RegisterRequestDto;
import com.project.Chat_Application.Dto.SecurityDto.Response.AuthResponseDto;
import com.project.Chat_Application.Entities.User;
import com.project.Chat_Application.Exceptions.ResourceNotFoundException;
import com.project.Chat_Application.Repositories.UsersRepository;
import com.project.Chat_Application.Security.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthControllers {
    private static final Logger log = LoggerFactory.getLogger(AuthControllers.class);

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthControllers(UsersRepository usersRepository, PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto dto) {
        if (usersRepository.findByUsername(dto.getUsername()).isPresent()) {
            log.info("Registration Unsuccessful, User already taken {}",dto.getUsername());
            throw new IllegalArgumentException("Username already taken");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        User saved = usersRepository.save(user);

        String token = jwtUtil.generateToken(saved.getUsername());
        log.info("Registration Successful! {}" , dto.getUsername());
        return new ResponseEntity<>(
                new AuthResponseDto (saved.getId(),saved.getUsername(),token, "Bearer" ),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
            );
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for username: {}", dto.getUsername());
            throw e;
        }

        User user = usersRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtUtil.generateToken(user.getUsername());
        log.info("Login successful: {}", dto.getUsername());
        return ResponseEntity.ok(new AuthResponseDto (user.getId(),user.getUsername(),token, "Bearer" ));
    }
}
