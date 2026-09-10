import { useCallback, useEffect, useState } from 'react';
import { ArrowUpRight, Plus, LogOut, UserRound, Menu, X } from 'lucide-react';
import { request, logout } from './lib/api';
import { Brand, Notice, Spinner, AuctionArt, AuthGate } from './components/ui';
import Browse from './pages/Browse';
import AuctionDetail from './pages/AuctionDetail';
import { CreateAuction, Register, Profile } from './pages/Forms';

export const CURRENCY = import.meta.env.VITE_CURRENCY || 'EUR';

function readRoute() {
  return window.location.hash.slice(1) || '/';
}

export default function App() {
  const [route, setRoute] = useState(readRoute);
  const [user, setUser] = useState(null);
  const [checking, setChecking] = useState(true);
  const [sessionError, setSessionError] = useState('');
  const [toast, setToast] = useState(null);
  const [menu, setMenu] = useState(false);
  const [signingOut, setSigningOut] = useState(false);
  const notify = useCallback((message, tone = 'success') => setToast({ message, tone }), []);
  const navigate = useCallback((path) => {
    window.location.hash = path;
  }, []);
  const login = useCallback(() => {
    try {
      sessionStorage.setItem('going-return', readRoute() === '/register' ? '/' : readRoute());
    } catch {
      /* Storage may be disabled. */
    }
    window.location.assign('/auth/login');
  }, []);

  const checkSession = useCallback(async () => {
    setChecking(true);
    setSessionError('');
    try {
      setUser((await request('/account-service/api/me', { anonymous: true })).data);
    } catch (error) {
      setUser(null);
      if (error.status !== 401) setSessionError(error.message);
    } finally {
      setChecking(false);
    }
  }, []);

  useEffect(() => {
    checkSession();
    const change = () => {
      setRoute(readRoute());
      setMenu(false);
      window.scrollTo(0, 0);
    };
    const expired = () => {
      setUser(null);
      setSessionError('Your session has ended. Sign in again to continue.');
    };
    window.addEventListener('hashchange', change);
    window.addEventListener('session-expired', expired);
    try {
      const saved = sessionStorage.getItem('going-return');
      if (saved) {
        sessionStorage.removeItem('going-return');
        if (saved.startsWith('/') && !saved.startsWith('//')) navigate(saved);
      }
    } catch {
      /* Storage may be disabled. */
    }
    return () => {
      window.removeEventListener('hashchange', change);
      window.removeEventListener('session-expired', expired);
    };
  }, [checkSession, navigate]);

  useEffect(() => {
    if (!toast || toast.tone !== 'success') return;
    const timer = setTimeout(() => setToast(null), 6500);
    return () => clearTimeout(timer);
  }, [toast]);

  async function signOut() {
    setSigningOut(true);
    try {
      await logout();
    } catch (error) {
      notify(error.message, 'error');
      setSigningOut(false);
    }
  }

  const props = { user, notify, navigate, currency: CURRENCY };
  const detailId = route.match(/^\/auctions\/([a-f0-9-]{36})$/i)?.[1];
  const isBrowse = ['/', '/selling', '/bidding'].includes(route);

  return (
    <>
      <a
        className="skip-link"
        href="#main"
        onClick={(event) => {
          event.preventDefault();
          document.getElementById('main').focus();
        }}
      >
        Skip to content
      </a>
      <header className="site-header">
        <div className="header-inner">
          <Brand />
          <button
            className="icon-button mobile-menu"
            onClick={() => setMenu(!menu)}
            aria-label={menu ? 'Close navigation' : 'Open navigation'}
            aria-expanded={menu}
          >
            {menu ? <X /> : <Menu />}
          </button>
          <nav className={menu ? 'nav open' : 'nav'} aria-label="Main navigation">
            <a href="#/" className={route === '/' ? 'active' : ''}>
              Explore auctions
            </a>
            {user && (
              <>
                <a href="#/selling" className={route === '/selling' ? 'active' : ''}>
                  My listings
                </a>
                <a href="#/bidding" className={route === '/bidding' ? 'active' : ''}>
                  My bidding
                </a>
              </>
            )}
          </nav>
          <div className="header-actions">
            {user ? (
              <>
                <a href="#/create" className="button primary compact">
                  <Plus size={17} />
                  <span>List an item</span>
                </a>
                <a
                  className={`avatar ${route === '/profile' ? 'selected' : ''}`}
                  href="#/profile"
                  title="My account"
                  aria-label="My account"
                >
                  {user.username?.slice(0, 1).toUpperCase() || <UserRound size={18} />}
                </a>
                <button
                  className="icon-button logout-button"
                  onClick={signOut}
                  disabled={signingOut}
                  title="Sign out"
                  aria-label="Sign out"
                >
                  <LogOut size={18} />
                </button>
              </>
            ) : (
              !checking && (
                <>
                  <button className="text-button" onClick={login}>
                    Sign in
                  </button>
                  <a href="#/register" className="button primary compact">
                    Get started <ArrowUpRight size={16} />
                  </a>
                </>
              )
            )}
          </div>
        </div>
      </header>
      <main id="main" tabIndex={-1} className="main-shell">
        {sessionError && (
          <Notice onClose={() => setSessionError('')}>
            {sessionError}{' '}
            <button className="text-button" onClick={checkSession}>
              Retry connection
            </button>
          </Notice>
        )}
        {checking ? (
          <div className="page-loading">
            <Spinner label="Getting things ready…" />
          </div>
        ) : route === '/register' ? (
          <Register {...props} onLogin={login} />
        ) : !user ? (
          <>
            <section className="hero">
              <div className="hero-copy">
                <span className="eyebrow">
                  <span className="live-dot" /> THE NEXT GREAT FIND
                </span>
                <h1>
                  Good things.
                  <br />
                  <em>Going, going.</em>
                </h1>
                <p>
                  A place for second chapters and first discoveries.
                  <br className="desktop-break" /> Find something you love. Make it yours.
                </p>
                <button className="button primary" onClick={login}>
                  Explore the auctions <ArrowUpRight size={18} />
                </button>
              </div>
              <AuctionArt />
            </section>
            <AuthGate onLogin={login} />
            <div className="how-it-works">
              {[
                ['01', 'Find your thing', 'Explore the listings and take a closer look.'],
                ['02', 'Make your move', 'Place a higher bid before the clock runs out.'],
                ['03', 'Watch your inbox', 'We’ll email you when you win or get outbid.'],
              ].map(([number, title, text]) => (
                <div key={number}>
                  <span>{number}</span>
                  <h3>{title}</h3>
                  <p>{text}</p>
                </div>
              ))}
            </div>
          </>
        ) : isBrowse ? (
          <Browse
            {...props}
            key={route}
            view={route === '/selling' ? 'selling' : route === '/bidding' ? 'bidding' : 'explore'}
          />
        ) : route === '/create' ? (
          <CreateAuction {...props} />
        ) : route === '/profile' ? (
          <Profile {...props} onLogout={signOut} />
        ) : detailId ? (
          <AuctionDetail {...props} key={detailId} id={detailId} />
        ) : (
          <section className="auth-gate">
            <h1>Nothing here just yet.</h1>
            <a href="#/" className="button primary">
              Back to auctions
            </a>
          </section>
        )}
      </main>
      <footer className="site-footer">
        <Brand />
        <p>Good things deserve a next chapter.</p>
        <span>Made for a little friendly competition.</span>
      </footer>
      {toast && (
        <div className="toast">
          <Notice tone={toast.tone} onClose={() => setToast(null)}>
            {toast.message}
          </Notice>
        </div>
      )}
    </>
  );
}
