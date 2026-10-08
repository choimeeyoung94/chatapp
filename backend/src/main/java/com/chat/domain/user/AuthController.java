package com.chat.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public AuthDto.UserRes signup(@RequestBody @jakarta.validation.Valid AuthDto.SignupReq req) {
        return authService.signup(req);
    }

    @PostMapping("/login")
    public AuthDto.LoginRes login(@RequestBody @jakarta.validation.Valid AuthDto.LoginReq req) {
        return authService.login(req);
    }

    @PostMapping("/guest")
    public AuthDto.GuestRes guest(@RequestBody @jakarta.validation.Valid AuthDto.GuestReq req) {
        return authService.guest(req);
    }

    @GetMapping("/me")
    public AuthDto.UserRes me(@AuthenticationPrincipal User user) {
        return AuthDto.UserRes.from(user);
    }
}
