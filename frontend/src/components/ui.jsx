import { useEffect, useRef, useState } from 'react';
import { ArrowUpRight, Gavel, Package, X, LoaderCircle } from 'lucide-react';
import { imagePath } from '../lib/auction';

export function Spinner({ label = 'Loading…' }) {
  return (
    <span className="loading" role="status">
      <LoaderCircle size={18} className="spin" />
      {label}
    </span>
  );
}

export function Notice({ children, tone = 'error', onClose }) {
  if (!children) return null;
  return (
    <div className={`notice ${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      <span>{children}</span>
      {onClose && (
        <button className="icon-button" onClick={onClose} aria-label="Dismiss message">
          <X size={17} />
        </button>
      )}
    </div>
  );
}

export function Empty({ title, children, action }) {
  return (
    <div className="empty-state">
      <span className="empty-icon">
        <Package size={30} strokeWidth={1.3} />
      </span>
      <h3>{title}</h3>
      <p>{children}</p>
      {action}
    </div>
  );
}

export function ItemImage({ item, large = false }) {
  const path = imagePath(item?.itemImageLocation);
  const [failed, setFailed] = useState(false);
  useEffect(() => setFailed(false), [path]);
  return (
    <div className={`item-image ${large ? 'large' : ''}`}>
      {path && !failed ? (
        <img
          src={path}
          alt={item?.itemName || 'Auction item'}
          loading="lazy"
          onError={() => setFailed(true)}
        />
      ) : (
        <div className="image-placeholder">
          <Package size={large ? 78 : 52} strokeWidth={0.85} />
          <span>No photo yet</span>
        </div>
      )}
    </div>
  );
}

export function Dialog({ title, children, onClose, busy = false }) {
  const ref = useRef(null);
  useEffect(() => {
    ref.current.showModal();
  }, []);
  return (
    <dialog
      ref={ref}
      className="dialog"
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onClose();
      }}
    >
      <div className="dialog-heading">
        <h2>{title}</h2>
        <button className="icon-button" aria-label="Close dialog" onClick={onClose} disabled={busy}>
          <X size={20} />
        </button>
      </div>
      {children}
    </dialog>
  );
}

export function Brand() {
  return (
    <a href="#/" className="brand" aria-label="Going home">
      <span>
        <Gavel size={21} />
      </span>
      going<span className="brand-dot">.</span>
    </a>
  );
}

export function AuctionArt() {
  return (
    <div className="auction-art" aria-hidden="true">
      <div className="art-orbit orbit-one" />
      <div className="art-orbit orbit-two" />
      <div className="art-tag">
        <span className="live-dot" /> A little competition.
        <br />
        <strong>A great new find.</strong>
      </div>
      <div className="art-disc">
        <Gavel size={94} strokeWidth={1.05} />
      </div>
      <span className="art-star">✳</span>
      <div className="art-stamp">
        NEXT CHAPTER
        <br />
        <ArrowUpRight size={24} />
        <br />
        STARTS HERE
      </div>
    </div>
  );
}

export function AuthGate({ onLogin, title = 'Your next find is waiting.' }) {
  return (
    <section className="auth-gate">
      <span className="eyebrow">MAKE YOURSELF AT HOME</span>
      <h2>{title}</h2>
      <p>Sign in to explore the auctions, place a bid, or give something good a new home.</p>
      <div className="button-row">
        <button className="button primary" onClick={onLogin}>
          Sign in <ArrowUpRight size={17} />
        </button>
        <a className="button secondary" href="#/register">
          Create an account
        </a>
      </div>
    </section>
  );
}
