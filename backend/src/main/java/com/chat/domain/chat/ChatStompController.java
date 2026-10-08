package com.chat.domain.chat;

import com.chat.domain.user.User;
import com.chat.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatStompController {
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final com.chat.global.security.JwtTokenProvider jwtTokenProvider;

    @MessageMapping("/chat.send")
    public void send(@Payload ChatDto.SendReq req, SimpMessageHeaderAccessor headers, Principal principal) {
        User me = resolveUser(headers, principal);
        String guestToken = headers.getFirstNativeHeader("X-Guest-Token");
        String guestNickname = headers.getFirstNativeHeader("X-Guest-Nickname");
        chatService.send(req.roomId(), me, guestToken, guestNickname, req);
    }

    @MessageMapping("/chat.join")
    public void join(@Payload ChatDto.SendReq req, SimpMessageHeaderAccessor headers, Principal principal) {
        // 입장 브로드캐스트는 REST join에서 처리, 소켓 join은 무시(호환용)
    }

    @MessageMapping("/chat.leave")
    public void leave(@Payload ChatDto.SendReq req, SimpMessageHeaderAccessor headers, Principal principal) {
    }

    private User resolveUser(SimpMessageHeaderAccessor h, Principal p) {
        String auth = h.getFirstNativeHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                if (jwtTokenProvider.validate(token)) {
                    Long id = jwtTokenProvider.getUserId(token);
                    return userRepository.findById(id).orElse(null);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
