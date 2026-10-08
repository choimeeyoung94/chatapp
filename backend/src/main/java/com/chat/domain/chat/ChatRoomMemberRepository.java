package com.chat.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    List<ChatRoomMember> findByRoomId(Long roomId);
    List<ChatRoomMember> findByUserId(Long userId);
    Optional<ChatRoomMember> findByRoomIdAndUserId(Long roomId, Long userId);
    Optional<ChatRoomMember> findByRoomIdAndGuestToken(Long roomId, String guestToken);
    long countByRoomId(Long roomId);
    void deleteByRoomIdAndUserId(Long roomId, Long userId);

    @Query("select m from ChatRoomMember m join fetch m.room where m.user.id = :userId")
    List<ChatRoomMember> findWithRoomByUserId(Long userId);
}
