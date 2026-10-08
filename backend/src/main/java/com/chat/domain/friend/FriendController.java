package com.chat.domain.friend;

import com.chat.domain.user.AuthDto;
import com.chat.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {
    private final FriendService friendService;

    @PostMapping("/request")
    public Map<String, String> request(@AuthenticationPrincipal User me, @RequestBody Map<String, Long> body) {
        friendService.request(me, body.get("addresseeId"));
        return Map.of("result", "ok");
    }

    @PostMapping("/{id}/accept")
    public Map<String, String> accept(@AuthenticationPrincipal User me, @PathVariable Long id) {
        friendService.accept(me, id);
        return Map.of("result", "ok");
    }

    @PostMapping("/{id}/reject")
    public Map<String, String> reject(@AuthenticationPrincipal User me, @PathVariable Long id) {
        friendService.reject(me, id);
        return Map.of("result", "ok");
    }

    @GetMapping
    public List<AuthDto.UserRes> list(@AuthenticationPrincipal User me) {
        return friendService.friends(me);
    }

    @GetMapping("/requests")
    public List<FriendService.RequestRes> requests(@AuthenticationPrincipal User me) {
        return friendService.requests(me);
    }
}
