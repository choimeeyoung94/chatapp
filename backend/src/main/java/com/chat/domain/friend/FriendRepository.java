package com.chat.domain.friend;

import com.chat.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    Optional<Friend> findByRequesterAndAddressee(User requester, User addressee);
    List<Friend> findByAddresseeAndStatus(User addressee, FriendStatus status);
    @Query("select f from Friend f join fetch f.requester join fetch f.addressee where (f.requester.id = :userId or f.addressee.id = :userId) and f.status = 'ACCEPTED'")
    List<Friend> findAcceptedFriends(Long userId);
}
