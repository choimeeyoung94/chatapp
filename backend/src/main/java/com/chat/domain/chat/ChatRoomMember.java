package com.chat.domain.chat;

import com.chat.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room_members",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"room_id", "user_id"}),
        @UniqueConstraint(columnNames = {"room_id", "guest_token"})
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EntityListeners(AuditingEntityListener.class)
public class ChatRoomMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "guest_nickname", length = 50)
    private String guestNickname;

    @Column(name = "guest_token", length = 64)
    private String guestToken;

    @Column(length = 10)
    @Builder.Default
    private String role = "MEMBER";

    @Column(name = "last_read_message_id")
    @Builder.Default
    private Long lastReadMessageId = 0L;

    @CreatedDate
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    public String displayName() {
        if (user != null) return user.getNickname();
        return guestNickname != null ? guestNickname : "익명";
    }
}
