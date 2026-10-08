package com.chat.domain.chat;

import com.chat.domain.user.User;
import com.chat.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository roomRepo;
    private final ChatRoomMemberRepository memberRepo;
    private final MessageRepository messageRepo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messaging;

    private String newInviteCode() {
        return "OPEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional
    public ChatDto.RoomRes createDm(User me, Long targetId) {
        if (me.getId().equals(targetId)) throw new IllegalArgumentException("자기 자신과 DM 불가");
        User target = userRepo.findById(targetId).orElseThrow(() -> new IllegalArgumentException("상대 없음"));
        for (ChatRoomMember m : memberRepo.findByUserId(me.getId())) {
            ChatRoom r = m.getRoom();
            if (r.getType() == ChatRoomType.DM) {
                boolean hasTarget = memberRepo.findByRoomId(r.getId()).stream()
                        .anyMatch(x -> x.getUser() != null && x.getUser().getId().equals(targetId));
                if (hasTarget) return toRoomRes(r, me.getId(), null);
            }
        }
        long a = Math.min(me.getId(), targetId), b = Math.max(me.getId(), targetId);
        ChatRoom room = roomRepo.save(ChatRoom.builder()
                .name("DM:" + a + ":" + b).type(ChatRoomType.DM).owner(me).build());
        memberRepo.save(ChatRoomMember.builder().room(room).user(me).role("OWNER").build());
        memberRepo.save(ChatRoomMember.builder().room(room).user(target).role("MEMBER").build());
        saveSystem(room, me.getNickname() + "님이 대화를 시작했습니다", MessageType.ENTER);
        return toRoomRes(room, me.getId(), null);
    }

    @Transactional
    public ChatDto.RoomRes createGroup(User me, String name, List<Long> memberIds) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("방 이름 필요");
        ChatRoom room = roomRepo.save(ChatRoom.builder().name(name).type(ChatRoomType.GROUP).owner(me).build());
        memberRepo.save(ChatRoomMember.builder().room(room).user(me).role("OWNER").build());
        if (memberIds != null) for (Long uid : memberIds.stream().distinct().toList()) {
            if (uid.equals(me.getId())) continue;
            userRepo.findById(uid).ifPresent(u ->
                memberRepo.save(ChatRoomMember.builder().room(room).user(u).role("MEMBER").build()));
        }
        saveSystem(room, me.getNickname() + "님이 그룹방을 만들었습니다", MessageType.ENTER);
        return toRoomRes(room, me.getId(), null);
    }

    @Transactional
    public ChatDto.RoomRes createOpen(User me, String name, String desc, Integer max) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("방 이름 필요");
        ChatRoom room = roomRepo.save(ChatRoom.builder()
                .name(name).description(desc).type(ChatRoomType.OPEN)
                .anonymous(true).maxMembers(max == null ? 500 : max)
                .owner(me).inviteCode(newInviteCode()).build());
        memberRepo.save(ChatRoomMember.builder().room(room).user(me).role("OWNER").build());
        saveSystem(room, "오픈채팅방이 개설되었습니다", MessageType.ENTER);
        return toRoomRes(room, me.getId(), null);
    }

    @Transactional(readOnly = true)
    public List<ChatDto.RoomRes> myRooms(User me) {
        return memberRepo.findWithRoomByUserId(me.getId()).stream()
                .map(m -> toRoomRes(m.getRoom(), me.getId(), m.getLastReadMessageId())).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatDto.RoomRes> openRooms() {
        return roomRepo.findAll().stream()
                .filter(r -> r.getType() == ChatRoomType.OPEN)
                .map(r -> toRoomRes(r, null, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatDto.MemberRes> members(Long roomId) {
        assertMember(roomId, null, null);
        return memberRepo.findByRoomId(roomId).stream()
                .map(m -> new ChatDto.MemberRes(m.getId(),
                        m.getUser() != null ? m.getUser().getId() : null,
                        m.getUser() != null ? m.getUser().getNickname()
                                : (m.getGuestNickname() != null ? "(익명) " + m.getGuestNickname() : "익명"),
                        m.getRole(), m.getUser() == null)).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatDto.MessageRes> messages(Long roomId, Long before, int size, User me, String guestToken) {
        assertMember(roomId, me, guestToken);
        List<Message> list = messageRepo.findRecent(roomId, before, PageRequest.of(0, Math.min(size, 100)));
        return list.stream().map(ChatDto.MessageRes::from).toList();
    }

    @Transactional
    public ChatDto.RoomRes join(User me, String guestToken, String guestNickname, String inviteCode) {
        ChatRoom room = roomRepo.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("초대코드 오류"));
        if (memberRepo.countByRoomId(room.getId()) >= room.getMaxMembers())
            throw new IllegalArgumentException("방이 가득 찼습니다");
        String who;
        if (me != null) {
            if (memberRepo.findByRoomIdAndUserId(room.getId(), me.getId()).isPresent())
                return toRoomRes(room, me.getId(), null);
            memberRepo.save(ChatRoomMember.builder().room(room).user(me).role("MEMBER").build());
            who = me.getNickname();
        } else {
            if (guestToken == null) throw new IllegalArgumentException("게스트 토큰 필요 (POST /api/auth/guest)");
            if (memberRepo.findByRoomIdAndGuestToken(room.getId(), guestToken).isPresent())
                return toRoomRes(room, null, null);
            memberRepo.save(ChatRoomMember.builder().room(room).guestToken(guestToken)
                    .guestNickname(guestNickname != null ? guestNickname : "익명").role("MEMBER").build());
            who = guestNickname != null ? guestNickname : "익명";
        }
        broadcast(room.getId(), saveSystem(room, who + "님이 입장했습니다", MessageType.ENTER));
        return toRoomRes(room, me != null ? me.getId() : null, null);
    }

    @Transactional
    public void leave(Long roomId, User me, String guestToken) {
        ChatRoom room = roomRepo.findById(roomId).orElseThrow(() -> new IllegalArgumentException("방 없음"));
        String who = "?";
        if (me != null) {
            ChatRoomMember m = memberRepo.findByRoomIdAndUserId(roomId, me.getId())
                    .orElseThrow(() -> new IllegalArgumentException("멤버 아님"));
            who = m.displayName();
            memberRepo.delete(m);
        } else if (guestToken != null) {
            ChatRoomMember m = memberRepo.findByRoomIdAndGuestToken(roomId, guestToken)
                    .orElseThrow(() -> new IllegalArgumentException("멤버 아님"));
            who = m.displayName();
            memberRepo.delete(m);
        }
        broadcast(roomId, saveSystem(room, who + "님이 퇴장했습니다", MessageType.LEAVE));
    }

    @Transactional
    public void read(Long roomId, User me, String guestToken, Long lastId) {
        ChatRoomMember m = resolveMember(roomId, me, guestToken);
        if (m == null) throw new IllegalArgumentException("멤버 아님");
        m.setLastReadMessageId(lastId);
    }

    @Transactional
    public ChatDto.MessageRes send(Long roomId, User me, String guestToken, String guestNickname, ChatDto.SendReq req) {
        ChatRoom room = roomRepo.findById(roomId).orElseThrow(() -> new IllegalArgumentException("방 없음"));
        ChatRoomMember m = resolveMember(roomId, me, guestToken);
        if (m == null) throw new IllegalArgumentException("방 멤버가 아닙니다. 먼저 입장하세요");
        MessageType type = MessageType.TEXT;
        if (req.type() != null) try { type = MessageType.valueOf(req.type()); } catch (Exception ignored) {}
        if (type == MessageType.ENTER || type == MessageType.LEAVE) type = MessageType.TEXT;
        Message msg = Message.builder()
                .room(room)
                .sender(me)
                .senderName(me != null ? me.getNickname()
                        : (guestNickname != null ? guestNickname : m.displayName()))
                .guestToken(me == null ? (guestToken != null ? guestToken : m.getGuestToken()) : null)
                .content(req.content()).type(type)
                .fileUrl(req.fileUrl()).fileName(req.fileName())
                .replyTo(req.replyToId() != null ? messageRepo.findById(req.replyToId()).orElse(null) : null)
                .build();
        if (req.fileUrl() != null && type == MessageType.TEXT) {
            String f = (req.fileName() != null ? req.fileName() : req.fileUrl()).toLowerCase();
            if (f.matches(".*\\.(png|jpe?g|gif|webp|svg)(\\?.*)?$")) msg.setType(MessageType.IMAGE);
            else msg.setType(MessageType.FILE);
        }
        ChatDto.MessageRes res = ChatDto.MessageRes.from(messageRepo.save(msg));
        broadcast(roomId, res);
        return res;
    }

    private Message saveSystem(ChatRoom room, String content, MessageType type) {
        return messageRepo.save(Message.builder()
                .room(room).senderName("system").content(content).type(type).build());
    }

    private void broadcast(Long roomId, Message m) {
        broadcast(roomId, ChatDto.MessageRes.from(m));
    }

    private void broadcast(Long roomId, ChatDto.MessageRes res) {
        try { messaging.convertAndSend("/topic/room." + roomId, res); } catch (Exception ignored) {}
    }

    private ChatRoomMember resolveMember(Long roomId, User me, String guestToken) {
        if (me != null) return memberRepo.findByRoomIdAndUserId(roomId, me.getId()).orElse(null);
        if (guestToken != null) return memberRepo.findByRoomIdAndGuestToken(roomId, guestToken).orElse(null);
        return null;
    }

    private void assertMember(Long roomId, User me, String guestToken) {
        if (me == null && guestToken == null) return;
        if (resolveMember(roomId, me, guestToken) == null && me != null)
            throw new IllegalArgumentException("방 멤버가 아닙니다");
    }

    private ChatDto.RoomRes toRoomRes(ChatRoom r, Long myId, Long lastRead) {
        List<Message> latest = messageRepo.findLatest(r.getId(), PageRequest.of(0, 1));
        Message lm = latest.isEmpty() ? null : latest.get(0);
        long unread = (myId != null && lastRead != null)
                ? messageRepo.countByRoomIdAndIdGreaterThan(r.getId(), lastRead) : 0;
        return new ChatDto.RoomRes(r.getId(), r.getName(), r.getType().name(),
                r.getDescription(), r.getInviteCode(),
                (int) memberRepo.countByRoomId(r.getId()),
                lm != null ? lm.getContent() : null,
                lm != null ? lm.getCreatedAt() : null, unread);
    }
}
