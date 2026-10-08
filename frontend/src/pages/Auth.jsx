import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { api } from '../api/client.js';

export function Login() {
  const [username, setUsername] = useState('user1');
  const [password, setPassword] = useState('1234');
  const [err, setErr] = useState('');
  const { login } = useAuth();
  const nav = useNavigate();

  const submit = async (e) => {
    e.preventDefault();
    setErr('');
    try { await login(username, password); nav('/'); }
    catch (e2) { setErr(e2.response?.data?.error || '로그인 실패'); }
  };

  const guest = async () => {
    const nick = prompt('익명 닉네임을 입력하세요', '익명' + Math.floor(Math.random() * 1000));
    if (!nick) return;
    const r = await api.post('/auth/guest', { nickname: nick });
    localStorage.setItem('guestToken', r.data.guestToken);
    localStorage.setItem('guestNickname', r.data.nickname);
    nav('/?tab=open');
  };

  return (
    <div className="center">
      <form className="card" onSubmit={submit}>
        <h1>💬 ChatApp</h1>
        {err && <div className="err">{err}</div>}
        <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="아이디" />
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="비밀번호" />
        <button className="btn btn-main" type="submit">로그인</button>
        <Link to="/signup"><button className="btn btn-ghost" type="button">회원가입</button></Link>
        <button className="btn btn-ghost" type="button" onClick={guest}>게스트로 둘러보기</button>
        <p style={{ fontSize: 12, color: '#888' }}>기본계정: user1/1234, user2/1234</p>
      </form>
    </div>
  );
}

export function Signup() {
  const [f, setF] = useState({ username: '', password: '', nickname: '' });
  const [err, setErr] = useState('');
  const nav = useNavigate();
  const submit = async (e) => {
    e.preventDefault();
    try { await api.post('/auth/signup', f); nav('/login'); }
    catch (e2) { setErr(e2.response?.data?.error || '가입 실패'); }
  };
  return (
    <div className="center">
      <form className="card" onSubmit={submit}>
        <h1>회원가입</h1>
        {err && <div className="err">{err}</div>}
        <input placeholder="아이디" value={f.username} onChange={(e) => setF({ ...f, username: e.target.value })} />
        <input placeholder="비밀번호" type="password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} />
        <input placeholder="닉네임" value={f.nickname} onChange={(e) => setF({ ...f, nickname: e.target.value })} />
        <button className="btn btn-main" type="submit">가입하기</button>
        <Link to="/login"><button className="btn btn-ghost" type="button">로그인으로</button></Link>
      </form>
    </div>
  );
}
