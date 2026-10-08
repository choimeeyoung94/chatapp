package com.chat.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;

    @GetMapping("/api/users/search")
    public List<AuthDto.UserRes> search(@RequestParam String keyword,
                                        @AuthenticationPrincipal User me) {
        return userRepository.findByNicknameContainingOrUsernameContaining(keyword, keyword)
                .stream().filter(u -> !u.getId().equals(me.getId()))
                .map(AuthDto.UserRes::from).toList();
    }
}
