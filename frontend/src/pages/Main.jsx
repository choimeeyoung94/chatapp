import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';

export function Main() {
  const [tab, setTab] = useState('chat');
  const [params] = useSearchParams();
  const [rooms, setRooms] = useState([]);
  const [openRooms, setOpenRooms] = useState([]);
  const [friends, setFriends] = useState([]);
  const [requests, setRequests] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [searchRes, setSearchRes] = useState([]);
  const [inviteCode, setInviteCode] = useState('');
  const [modal, setModal] = useState(null);
  const [form, setForm] = useState({ name: '', memberIds: '', description: '' });
  const { user, logout } = useAuth();
  const nav = useNavigate();

  useEffect(() => {
    const t = params.get('tab');
    if (t) setTab(t);
  }, [params]);

  const load = async () => {
    try {
      const r = await api.get('/chat/rooms');
      setRooms(r.data);
    } catch {}
    try {
      const r = await api.get('/chat/rooms/open');
      setOpenRooms(r.data);
    } catch {}
    if (localStorage.getItem('token')) {
      try { setFriends((await api.get('/friends')).data); } catch {}
      try { setRequests((await api.get('/friends/requests')).data); } catch {}
    }
  };
  useEffect(() => { load(); }, [tab]);

  const search = async () => {
    if (!keyword.trim()) return;
    const r = await api.get('/users/search', { params: { keyword } });
    setSearchRes(r.data);
  };

  const startDm = async (targetUserId) => {
    const r = await api.post('/chat/rooms/dm', { targetUserId });
    nav(`/room/${r.data.id}`);
  };

  const createGroup = async () => {
    const memberIds = form.memberIds.split(',').map((s) => Number(s.trim())).filter(Boolean);
    const r = await api.post('/chat/rooms/group', { name: form.name, memberIds });
    setModal(null); load(); nav(`/room/${r.data.id}`);
  };

  const createOpen = async () => {
    const r = await api.post('/chat/rooms/open', { name: form.name, description: form.description });
    setModal(null); load();
  };

  const joinByCode = async () => {
    if (!inviteCode.trim()) return;
    // 익명인데 게스트 토큰 없으면 발급
    if (!localStorage.getItem('token') && !localStorage.getItem('guestToken')) {
      const nick = prompt('익명 닉네임 입력', '익명' + Math.floor(Math.random() * 1000));
      if (!nick) return;
      const g = await api.post('/auth/guest', { nickname: nick });
      localStorage.setItem('guestToken', g.data.guestToken);
      localStorage.setItem('guestNickname', g.data.nickname);
    }
    const r = await api.post('/chat/rooms/join', { inviteCode: inviteCode.trim() });
    nav(`/room/${r.data.id}`);
  };

  return (
    <div className="layout">
      <div className="sidebar">
        <button className={tab === 'friends' ? 'active' : ''} onClick={() => setTab('friends')} title="친구">👤</button>
        <button className={tab === 'chat' ? 'active' : ''} onClick={() => setTab('chat')} title="채팅">💬</button>
        <button className={tab === 'open' ? 'active' : ''} onClick={() => setTab('open')} title="오픈채팅">🌐</button>
        <div style={{ flex: 1 }} />
        {user ? <button onClick={() => { logout(); nav('/login'); }} title="로그아웃">🚪</button>
              : <button onClick={() => nav('/login')} title="로그인">🔑</button>}
      </div>

      <div className="list">
        {tab === 'chat' && (
          <>
            <h2>채팅 <span>
              <button onClick={() => setModal('group')}>+그룹</button>{' '}
              <button onClick={() => setModal('open')}>+오픈</button>
            </span></h2>
            {rooms.map((r) => (
              <div key={r.id} className="room" onClick={() => nav(`/room/${r.id}`)}>
                <div className="avatar">{r.name?.[0] || '💬'}</div>
                <div className="meta">
                  <div className="t"><span>{r.name} {r.type === 'OPEN' ? '(오픈)' : ''}</span><span>{r.unreadCount > 0 && <span className="badge">{r.unreadCount}</span>}</span></div>
                  <div className="s">{r.lastMessage || '대화를 시작하세요'} · {r.memberCount}명</div>
                </div>
              </div>
            ))}
            {rooms.length === 0 && <p style={{ padding: 16, color: '#888' }}>참여 중인 방이 없습니다. 친구 탭에서 DM을 시작하세요.</p>}
          </>
        )}

        {tab === 'friends' && (
          <>
            <h2>친구 {requests.length > 0 && `(${requests.length} 요청)`}</h2>
            <div style={{ padding: '0 16px 8px', display: 'flex', gap: 6 }}>
              <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="ID/닉네임 검색" style={{ flex: 1, padding: 8 }} />
              <button onClick={search}>검색</button>
            </div>
            {searchRes.map((u) => (
              <div key={u.id} className="room">
                <div className="avatar">{u.nickname?.[0]}</div>
                <div className="meta"><div className="t">{u.nickname} <span style={{ fontWeight: 400, color: '#888' }}>@{u.username}</span></div></div>
                <button onClick={async () => { await api.post('/friends/request', { addresseeId: u.id }); alert('요청 전송'); }}>추가</button>
              </div>
            ))}
            {requests.map((rq) => (
              <div key={rq.id} className="room">
                <div className="avatar">{rq.requester.nickname?.[0]}</div>
                <div className="meta"><div className="t">{rq.requester.nickname}님의 요청</div></div>
                <button onClick={async () => { await api.post(`/friends/${rq.id}/accept`); load(); }}>수락</button>
              </div>
            ))}
            <div style={{ padding: '8px 16px', fontWeight: 700 }}>내 친구 {friends.length}</div>
            {friends.map((f) => (
              <div key={f.id} className="room">
                <div className="avatar">{f.nickname?.[0]}</div>
                <div className="meta"><div className="t">{f.nickname}</div><div className="s">{f.statusMessage || ''}</div></div>
                <button onClick={() => startDm(f.id)}>1:1</button>
              </div>
            ))}
          </>
        )}

        {tab === 'open' && (
          <>
            <h2>오픈채팅</h2>
            <div style={{ padding: '0 16px 8px', display: 'flex', gap: 6 }}>
              <input value={inviteCode} onChange={(e) => setInviteCode(e.target.value)} placeholder="초대코드 (예: OPEN-001)" style={{ flex: 1, padding: 8 }} />
              <button onClick={joinByCode}>입장</button>
            </div>
            {openRooms.map((r) => (
              <div key={r.id} className="room" onClick={joinByCode}>
                <div className="avatar">🌐</div>
                <div className="meta">
                  <div className="t"><span>{r.name}</span><span>{r.memberCount}명</span></div>
                  <div className="s">{r.description || ''} · 코드: {r.inviteCode}</div>
                </div>
                <button onClick={async (e) => {
                  e.stopPropagation();
                  setInviteCode(r.inviteCode);
                  if (!localStorage.getItem('token') && !localStorage.getItem('guestToken')) {
                    const nick = prompt('익명 닉네임 입력', '익명' + Math.floor(Math.random() * 1000));
                    if (!nick) return;
                    const g = await api.post('/auth/guest', { nickname: nick });
                    localStorage.setItem('guestToken', g.data.guestToken);
                    localStorage.setItem('guestNickname', g.data.nickname);
                  }
                  const rr = await api.post('/chat/rooms/join', { inviteCode: r.inviteCode });
                  nav(`/room/${rr.data.id}`);
                }}>입장</button>
              </div>
            ))}
          </>
        )}
      </div>

      <div className="main"><div className="empty">방을 선택하거나 새로 만드세요 💬<br />{user ? `${user.nickname}님 환영합니다` : '게스트 모드 (오픈채팅 입장 가능)'}</div></div>

      {modal && (
        <div className="modal" onClick={() => setModal(null)}>
          <div className="box" onClick={(e) => e.stopPropagation()}>
            <h3>{modal === 'group' ? '그룹방 만들기' : '오픈방 만들기'}</h3>
            <input placeholder="방 이름" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            {modal === 'group'
              ? <input placeholder="멤버 userId 쉼표구분 (예: 2,3)" value={form.memberIds} onChange={(e) => setForm({ ...form, memberIds: e.target.value })} />
              : <textarea placeholder="방 소개" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />}
            <div className="row">
              <button className="btn btn-main" onClick={modal === 'group' ? createGroup : createOpen}>만들기</button>
              <button className="btn btn-ghost" onClick={() => setModal(null)}>취소</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
