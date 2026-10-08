package com.chat.domain.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("select m from Message m where m.room.id = :roomId and (:before is null or m.id < :before) order by m.id desc")
    List<Message> findRecent(Long roomId, Long before, Pageable pageable);

    @Query("select m from Message m where m.room.id = :roomId order by m.id desc")
    List<Message> findLatest(Long roomId, Pageable pageable);

    long countByRoomIdAndIdGreaterThan(Long roomId, Long id);
}
