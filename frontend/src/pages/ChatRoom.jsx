import { useEffect, useRef, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { api, fileUrl, API_BASE } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';

export function ChatRoom() {
  const { id } = useParams();
  const nav = useNavigate();
  const { user } = useAuth();
  const [msgs, setMsgs] = useState([]);
  const [text, setText] = useState('');
  const [members, setMembers] = useState([]);
  const [showMembers, setShowMembers] = useState(false);
  const [replyTo, setReplyTo] = useState(null);
  const bottomRef = useRef(null);
  const clientRef = useRef(null);
  const myName = user?.nickname || decodeURIComponent(localStorage.getItem('guestNickname') || '익명');

  const loadMsgs = async (before) => {
    const r = await api.get(`/chat/rooms/${id}/messages`, { params: { before, size: 30 } });
    const list = [...r.data].reverse();
    if (before) setMsgs((prev) => [...list, ...prev]);
    else {
      setMsgs(list);
      if (list.length) api.post(`/chat/rooms/${id}/read`, { lastReadMessageId: list[list.length - 1].id }).catch(() => {});
    }
  };

  useEffect(() => {
    loadMsgs();
    api.get(`/chat/rooms/${id}/members`).then((r) => setMembers(r.data)).catch(() => {});

    const token = localStorage.getItem('token');
    const sock = new SockJS(`${API_BASE}/ws`);
    const client = new Client({
      webSocketFactory: () => sock,
      reconnectDelay: 3000,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      onConnect: () => {
        client.subscribe(`/topic/room.${id}`, (frame) => {
          const m = JSON.parse(frame.body);
          setMsgs((prev) => [...prev, m]);
          api.post(`/chat/rooms/${id}/read`, { lastReadMessageId: m.id }).catch(() => {});
        });
      },
    });
    client.activate();
    clientRef.current = client;
    return () => { client.deactivate(); };
    // eslint-disable-next-line
  }, [id]);

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [msgs]);

  const send = async (e) => {
    e?.preventDefault();
    if (!text.trim() && !replyTo) return;
    const payload = { roomId: Number(id), content: text, type: 'TEXT', replyToId: replyTo?.id ?? null };
    // STOMP 우선, 실패시 REST 폴백
    try {
      const token = localStorage.getItem('token');
      const headers = {};
      if (token) headers.Authorization = `Bearer ${token}`;
      const gt = localStorage.getItem('guestToken');
      if (gt) headers['X-Guest-Token'] = gt;
      const gn = localStorage.getItem('guestNickname');
      if (gn) headers['X-Guest-Nickname'] = encodeURIComponent(gn);
      clientRef.current?.publish({
        destination: '/app/chat.send',
        headers,
        body: JSON.stringify(payload),
      });
      // STOMP는 에코를 구독으로 받으므로 REST 중복전송 방지: 연결 안 됐을 때만 REST
      if (!clientRef.current?.connected) await api.post(`/chat/rooms/${id}/send`, payload);
    } catch {
      await api.post(`/chat/rooms/${id}/send`, payload);
    }
    setText('');
    setReplyTo(null);
  };

  const upload = async (e) => {
    const f = e.target.files?.[0];
    if (!f) return;
    const fd = new FormData();
    fd.append('file', f);
    const r = await api.post('/files/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
    const isImg = /\.(png|jpe?g|gif|webp|svg)$/i.test(r.data.fileName);
    await api.post(`/chat/rooms/${id}/send`, {
      roomId: Number(id), content: r.data.fileName, type: isImg ? 'IMAGE' : 'FILE',
      fileUrl: r.data.url, fileName: r.data.fileName,
    });
    e.target.value = '';
  };

  const leave = async () => {
    if (!confirm('방을 나가시겠습니까?')) return;
    await api.post(`/chat/rooms/${id}/leave`);
    nav('/');
  };

  return (
    <div className="layout">
      <div className="main" style={{ height: '100vh' }}>
        <div className="chat-head">
          <Link to="/">←</Link>
          <span>채팅방 #{id}</span>
          <span style={{ flex: 1 }} />
          <button onClick={() => setShowMembers((v) => !v)}>멤버({members.length})</button>
          <button onClick={leave}>나가기</button>
        </div>
        {showMembers && (
          <div style={{ background: '#fff', padding: 12, borderBottom: '1px solid #eee', fontSize: 13 }}>
            {members.map((m) => <div key={m.id}>• {m.displayName} ({m.role})</div>)}
          </div>
        )}
        <div className="msgs">
          <button onClick={() => msgs.length && loadMsgs(msgs[0].id)} style={{ alignSelf: 'center' }}>이전 메시지 더보기</button>
          {msgs.map((m) =>
            m.type === 'ENTER' || m.type === 'LEAVE' ? (
              <div className="sys" key={m.id}>{m.content}</div>
            ) : (
              <div key={m.id} className={'bubble ' + (m.senderName === myName ? 'mine' : 'theirs')}>
                {m.senderName !== myName && <div className="who">{m.senderName}</div>}
                {(m.type === 'IMAGE' && m.fileUrl) ? (
                  <a href={fileUrl(m.fileUrl)} target="_blank" rel="noreferrer">
                    <img className="att" src={fileUrl(m.fileUrl)} alt={m.fileName} />
                  </a>
                ) : (m.type === 'FILE' && m.fileUrl) ? (
                  <a href={fileUrl(m.fileUrl)} target="_blank" rel="noreferrer">📎 {m.fileName || m.content}</a>
                ) : (
                  <div>{m.content}</div>
                )}
                <div style={{ fontSize: 10, opacity: .6, textAlign: 'right' }}>
                  <span onClick={() => setReplyTo(m)} style={{ cursor: 'pointer' }}>답장</span>
                </div>
              </div>
            )
          )}
          <div ref={bottomRef} />
        </div>
        {replyTo && <div className="toolbar">답장: {replyTo.content?.slice(0, 30)} <button onClick={() => setReplyTo(null)}>x</button></div>}
        <form className="inputbar" onSubmit={send}>
          <label style={{ alignSelf: 'center' }}>📎<input type="file" hidden onChange={upload} /></label>
          <input type="text" value={text} onChange={(e) => setText(e.target.value)} placeholder="메시지 입력" />
          <button type="submit">전송</button>
        </form>
      </div>
    </div>
  );
}
