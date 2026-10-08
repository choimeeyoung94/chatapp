package com.chat.domain.chat;

import com.chat.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {
    private final ChatService chatService;

    @PostMapping("/dm")
    public ChatDto.RoomRes dm(@AuthenticationPrincipal User me, @RequestBody ChatDto.DmReq req) {
        return chatService.createDm(me, req.targetUserId());
    }

    @PostMapping("/group")
    public ChatDto.RoomRes group(@AuthenticationPrincipal User me, @RequestBody ChatDto.GroupReq req) {
        return chatService.createGroup(me, req.name(), req.memberIds());
    }

    @PostMapping("/open")
    public ChatDto.RoomRes open(@AuthenticationPrincipal User me, @RequestBody ChatDto.OpenReq req) {
        return chatService.createOpen(me, req.name(), req.description(), req.maxMembers());
    }

    @GetMapping
    public List<ChatDto.RoomRes> myRooms(@AuthenticationPrincipal User me) {
        return chatService.myRooms(me);
    }

    @GetMapping("/open")
    public List<ChatDto.RoomRes> openRooms() {
        return chatService.openRooms();
    }

    @PostMapping("/join")
    public ChatDto.RoomRes join(@AuthenticationPrincipal User me,
                                @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
                                @RequestBody ChatDto.JoinReq req,
                                @RequestHeader(value = "X-Guest-Nickname", required = false) String guestNickname) {
        return chatService.join(me, guestToken, guestNickname, req.inviteCode());
    }

    @PostMapping("/{id}/leave")
    public Map<String, String> leave(@AuthenticationPrincipal User me,
                                     @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
                                     @PathVariable Long id) {
        chatService.leave(id, me, guestToken);
        return Map.of("result", "ok");
    }

    @GetMapping("/{id}/members")
    public List<ChatDto.MemberRes> members(@PathVariable Long id) {
        return chatService.members(id);
    }

    @GetMapping("/{id}/messages")
    public List<ChatDto.MessageRes> messages(@AuthenticationPrincipal User me,
                                             @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
                                             @PathVariable Long id,
                                             @RequestParam(required = false) Long before,
                                             @RequestParam(defaultValue = "30") int size) {
        return chatService.messages(id, before, size, me, guestToken);
    }

    @PostMapping("/{id}/read")
    public Map<String, String> read(@AuthenticationPrincipal User me,
                                    @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
                                    @PathVariable Long id, @RequestBody ChatDto.ReadReq req) {
        chatService.read(id, me, guestToken, req.lastReadMessageId());
        return Map.of("result", "ok");
    }

    @PostMapping("/{id}/send")
    public ChatDto.MessageRes sendRest(@AuthenticationPrincipal User me,
                                       @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
                                       @RequestHeader(value = "X-Guest-Nickname", required = false) String guestNickname,
                                       @PathVariable Long id, @RequestBody ChatDto.SendReq req) {
        return chatService.send(id, me, guestToken, guestNickname, req);
    }
}
