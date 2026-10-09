package com.chat.global.config;

import com.chat.domain.chat.*;
import com.chat.domain.user.User;
import com.chat.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final ChatRoomRepository roomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seed() {
        return args -> {
            // pipeline test 2026-10-09: no-op (full chain verify)
            seedData();
        };
    }

    private void seedData() {
            User u1 = userRepository.findByUsername("user1").orElseGet(() ->
                userRepository.save(User.builder().username("user1")
                    .passwordHash(passwordEncoder.encode("1234")).nickname("홍길동")
                    .statusMessage("안녕하세요!").build()));
            User u2 = userRepository.findByUsername("user2").orElseGet(() ->
                userRepository.save(User.builder().username("user2")
                    .passwordHash(passwordEncoder.encode("1234")).nickname("김철수").build()));
            userRepository.findByUsername("user3").orElseGet(() ->
                userRepository.save(User.builder().username("user3")
                    .passwordHash(passwordEncoder.encode("1234")).nickname("이영희").build()));
            if (roomRepository.findByInviteCode("OPEN-001").isEmpty()) {
                ChatRoom room = roomRepository.save(ChatRoom.builder()
                        .name("익명 고민상담소").description("고민을 나눠요")
                        .type(ChatRoomType.OPEN).anonymous(true).maxMembers(500)
                        .owner(u1).inviteCode("OPEN-001").build());
                memberRepository.save(ChatRoomMember.builder().room(room).user(u1).role("OWNER").build());
                memberRepository.save(ChatRoomMember.builder().room(room).user(u2).role("MEMBER").build());
            }
    }
}
