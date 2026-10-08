package com.chat.domain.chat;

import java.time.LocalDateTime;

public class ChatDto {
    public record DmReq(Long targetUserId) {}
    public record GroupReq(String name, java.util.List<Long> memberIds) {}
    public record OpenReq(String name, String description, Integer maxMembers) {}
    public record JoinReq(String inviteCode) {}
    public record ReadReq(Long lastReadMessageId) {}
    public record SendReq(Long roomId, String content, String type, String fileUrl, String fileName, Long replyToId) {}

    public record RoomRes(Long id, String name, String type, String description, String inviteCode,
                          int memberCount, String lastMessage, LocalDateTime lastMessageAt, long unreadCount) {}
    public record MessageRes(Long id, Long roomId, Long senderId, String senderName, String content,
                             String type, String fileUrl, String fileName, Long fileSize,
                             LocalDateTime createdAt) {
        public static MessageRes from(Message m) {
            return new MessageRes(m.getId(), m.getRoom().getId(),
                    m.getSender() != null ? m.getSender().getId() : null,
                    m.getSenderName(), m.getContent(),
                    m.getType().name(), m.getFileUrl(), m.getFileName(), m.getFileSize(),
                    m.getCreatedAt());
        }
    }
    public record MemberRes(Long id, Long userId, String displayName, String role, boolean anonymous) {}
}
