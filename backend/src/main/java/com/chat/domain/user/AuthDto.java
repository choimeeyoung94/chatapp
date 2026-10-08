package com.chat.domain.user;

import jakarta.validation.constraints.NotBlank;

public class AuthDto {
    public record SignupReq(@NotBlank String username, @NotBlank String password, @NotBlank String nickname) {}
    public record LoginReq(@NotBlank String username, @NotBlank String password) {}
    public record GuestReq(@NotBlank String nickname) {}
    public record UserRes(Long id, String username, String nickname, String profileImageUrl, String statusMessage) {
        public static UserRes from(User u) {
            return new UserRes(u.getId(), u.getUsername(), u.getNickname(), u.getProfileImageUrl(), u.getStatusMessage());
        }
    }
    public record LoginRes(String token, UserRes user) {}
    public record GuestRes(String guestToken, String nickname) {}
}
