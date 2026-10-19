package com.example.ERP_Api.Service;



import com.example.ERP_Api.Entity.User;
import com.example.ERP_Api.Repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record RegisterRequest(String name, String email, String password) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record AuthResponse(String token, String name, String email, String role) {
    }

    // Matches the create-account.html frontend: throws on duplicate email
    // so the controller can map it to a 409, matching the JS that already
    // checks response.status === 409.
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalStateException("An account with that email already exists: " + req.email());
        }

        String hash = passwordEncoder.encode(req.password());
        User user = new User(req.name(), req.email(), hash);
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    // Matches login.html: throws on bad credentials so the controller can
    // map it to a 401, matching the JS that already checks for that status.
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Incorrect email or password"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect email or password");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }
}