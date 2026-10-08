package com.chat.domain.friend;

import com.chat.domain.user.AuthDto;
import com.chat.domain.user.User;
import com.chat.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    @Transactional
    public void request(User me, Long addresseeId) {
        if (me.getId().equals(addresseeId)) throw new IllegalArgumentException("자기 자신은 추가할 수 없습니다");
        User target = userRepository.findById(addresseeId).orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        if (friendRepository.findByRequesterAndAddressee(me, target).isPresent()
                || friendRepository.findByRequesterAndAddressee(target, me).isPresent())
            throw new IllegalArgumentException("이미 요청했거나 친구입니다");
        friendRepository.save(Friend.builder().requester(me).addressee(target).status(FriendStatus.PENDING).build());
    }

    @Transactional
    public void accept(User me, Long friendId) {
        Friend f = friendRepository.findById(friendId).orElseThrow(() -> new IllegalArgumentException("요청 없음"));
        if (!f.getAddressee().getId().equals(me.getId())) throw new IllegalArgumentException("권한 없음");
        f.setStatus(FriendStatus.ACCEPTED);
    }

    @Transactional
    public void reject(User me, Long friendId) {
        Friend f = friendRepository.findById(friendId).orElseThrow(() -> new IllegalArgumentException("요청 없음"));
        if (!f.getAddressee().getId().equals(me.getId())) throw new IllegalArgumentException("권한 없음");
        f.setStatus(FriendStatus.REJECTED);
    }

    @Transactional(readOnly = true)
    public List<AuthDto.UserRes> friends(User me) {
        return friendRepository.findAcceptedFriends(me.getId()).stream()
                .map(f -> f.getRequester().getId().equals(me.getId()) ? f.getAddressee() : f.getRequester())
                .map(AuthDto.UserRes::from).toList();
    }

    public record RequestRes(Long id, AuthDto.UserRes requester) {}

    @Transactional(readOnly = true)
    public List<RequestRes> requests(User me) {
        return friendRepository.findByAddresseeAndStatus(me, FriendStatus.PENDING).stream()
                .map(f -> new RequestRes(f.getId(), AuthDto.UserRes.from(f.getRequester()))).toList();
    }
}
