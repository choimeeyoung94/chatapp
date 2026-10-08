import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext.jsx';
import { Login, Signup } from './pages/Auth.jsx';
import { Main } from './pages/Main.jsx';
import { ChatRoom } from './pages/ChatRoom.jsx';

function Guard({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="center">로딩...</div>;
  if (!user && !localStorage.getItem('guestToken')) return <Navigate to="/login" />;
  return children;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/" element={<Guard><Main /></Guard>} />
          <Route path="/room/:id" element={<Guard><ChatRoom /></Guard>} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
