package com.chat.domain.user;

import com.chat.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthDto.UserRes signup(AuthDto.SignupReq req) {
        if (userRepository.existsByUsername(req.username()))
            throw new IllegalArgumentException("이미 존재하는 아이디입니다");
        User u = User.builder()
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .nickname(req.nickname())
                .build();
        return AuthDto.UserRes.from(userRepository.save(u));
    }

    @Transactional(readOnly = true)
    public AuthDto.LoginRes login(AuthDto.LoginReq req) {
        User u = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다"));
        if (!passwordEncoder.matches(req.password(), u.getPasswordHash()))
            throw new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다");
        return new AuthDto.LoginRes(jwtTokenProvider.createToken(u.getId(), u.getUsername()), AuthDto.UserRes.from(u));
    }

    public AuthDto.GuestRes guest(AuthDto.GuestReq req) {
        String nick = req.nickname().trim();
        if (nick.isEmpty()) throw new IllegalArgumentException("닉네임을 입력하세요");
        return new AuthDto.GuestRes(UUID.randomUUID().toString().replace("-", ""), nick);
    }
}
