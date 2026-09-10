import { useRef, useState } from 'react';
import { ArrowLeft, ArrowUpRight, Check, ImagePlus, ShieldCheck, Trash2, X } from 'lucide-react';
import { request } from '../lib/api';
import { decimal, auctionDuration, MIN_AUCTION_SECONDS } from '../lib/auction';
import { Dialog, Notice, Spinner } from '../components/ui';

export function PhotoInput({ file, onChange, disabled = false }) {
  const [error, setError] = useState('');
  const inputRef = useRef(null);
  return (
    <div>
      <label className="photo-input">
        <ImagePlus size={27} strokeWidth={1.3} />
        <strong>{file ? file.name : 'Add a photo'}</strong>
        <span>JPG or PNG · up to 5 MB</span>
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png"
          aria-label="Item photo"
          disabled={disabled}
          onChange={(e) => {
            const selected = e.target.files?.[0];
            setError('');
            if (!selected) return;
            if (
              !['image/jpeg', 'image/png'].includes(selected.type) ||
              selected.size > 5 * 1024 * 1024
            ) {
              setError('Choose a JPG or PNG smaller than 5 MB.');
              e.target.value = '';
              return;
            }
            onChange(selected);
          }}
        />
      </label>
      {file && (
        <button
          type="button"
          className="text-button"
          disabled={disabled}
          onClick={() => {
            inputRef.current.value = '';
            onChange(null);
          }}
        >
          <X size={14} />
          Remove photo
        </button>
      )}
      <Notice>{error}</Notice>
    </div>
  );
}

export async function uploadPhoto(itemId, file) {
  const body = new FormData();
  body.append('file', file);
  return request(`/item-service/api/${itemId}/upload-image`, { method: 'POST', body });
}

export function CreateAuction({ notify, navigate, currency }) {
  const [file, setFile] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [stage, setStage] = useState('');

  async function create(event) {
    event.preventDefault();
    setError('');
    const form = new FormData(event.currentTarget);
    let startingPrice;
    let durationSeconds;
    try {
      startingPrice = decimal(form.get('price'));
      durationSeconds = auctionDuration(form.get('duration'));
    } catch (problem) {
      setError(problem.message);
      return;
    }
    setBusy(true);
    setStage('Creating your listing…');
    let created = false;
    let id;
    try {
      const result = await request('/bidding-service/api/create-with-item', {
        method: 'POST',
        body: {
          item: {
            itemName: form.get('title').trim(),
            itemDescription: form.get('description').trim(),
          },
          startingPrice,
          durationSeconds,
        },
      });
      created = true;
      id = result.location?.match(/\/details\/([a-f0-9-]{36})$/i)?.[1];
      if (file && id) {
        setStage('Adding your photo…');
        const { data } = await request(`/bidding-service/api/get/${id}`);
        await uploadPhoto(data.itemId, file);
      }
      notify('Your listing is live. Let the bidding begin.');
      navigate(id ? `/auctions/${id}` : '/selling');
    } catch (problem) {
      if (created) {
        notify(
          'Your listing was created, but the photo could not be added. You can retry from the listing page.',
          'info',
        );
        navigate(id ? `/auctions/${id}` : '/selling');
      } else
        setError(
          `${problem.message}${!problem.status || problem.status >= 500 ? ' Check My listings before submitting again in case the creation completed.' : ''}`,
        );
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <a className="back-link" href="#/">
        <ArrowLeft size={16} />
        Back to auctions
      </a>
      <div className="form-layout">
        <div className="form-intro">
          <span className="eyebrow">SOMETHING GOOD TO PASS ON</span>
          <h1>
            Give it a<br />
            <em>next chapter.</em>
          </h1>
          <p>A few details, a starting price, and a duration. Your next auction starts here.</p>
          <div className="tip-card">
            <ShieldCheck size={24} />
            <h3>You set the starting point.</h3>
            <p>
              Every offer must beat the current price. When time is up, we’ll email you the result.
            </p>
          </div>
        </div>
        <form className="form-panel" onSubmit={create}>
          <div className="panel-heading">
            <h2>Create a listing</h2>
            <span>Make it a good one.</span>
          </div>
          <Notice>{error}</Notice>
          <fieldset disabled={busy}>
            <label>
              Item name
              <input
                name="title"
                required
                maxLength={200}
                placeholder="e.g. A well-loved film camera"
              />
            </label>
            <label>
              Description
              <textarea
                name="description"
                required
                maxLength={4000}
                rows={5}
                placeholder="Tell its story. Include the condition and any details a bidder should know."
              />
            </label>
            <div className="form-row">
              <label>
                Starting price <span className="label-note">({currency})</span>
                <input
                  name="price"
                  required
                  inputMode="decimal"
                  placeholder="0.00"
                  pattern="[0-9]{1,17}(\.[0-9]{1,2})?"
                  title="A non-negative amount with up to two decimal places"
                />
              </label>
              <label>
                Duration (seconds)
                <input
                  name="duration"
                  type="number"
                  required
                  defaultValue={MIN_AUCTION_SECONDS}
                  min={MIN_AUCTION_SECONDS}
                  step={1}
                />
              </label>
            </div>
            <p className="field-help">
              At least one minute, with no maximum duration. The first bid must be higher than your
              starting price.
            </p>
            <div className="field-label">
              Item photo <span className="label-note">optional</span>
            </div>
            <PhotoInput file={file} onChange={setFile} disabled={busy} />
            <div className="form-footer">
              <span>Before the first offer, you can edit the price or change the duration.</span>
              <button className="button primary" type="submit">
                {busy ? (
                  <Spinner label={stage} />
                ) : (
                  <>
                    Publish listing <ArrowUpRight size={17} />
                  </>
                )}
              </button>
            </div>
          </fieldset>
        </form>
      </div>
    </>
  );
}

export function Register({ user, onLogin, navigate }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  async function register(event) {
    event.preventDefault();
    setError('');
    const fields = new FormData(event.currentTarget);
    if (fields.get('password') !== fields.get('confirm')) {
      setError('Your passwords don’t match.');
      return;
    }
    setBusy(true);
    try {
      await request('/auth/register', {
        method: 'POST',
        anonymous: true,
        body: {
          username: fields.get('username').trim(),
          email: fields.get('email').trim(),
          password: fields.get('password'),
        },
      });
      setSuccess(true);
    } catch (problem) {
      setError(problem.message);
    } finally {
      setBusy(false);
    }
  }
  if (user)
    return (
      <section className="auth-gate">
        <h1>You’re already at home.</h1>
        <p>You’re signed in as {user.username}.</p>
        <button className="button primary" onClick={() => navigate('/')}>
          Explore auctions
        </button>
      </section>
    );
  if (success)
    return (
      <section className="registration-success">
        <span className="success-icon">
          <Check size={32} />
        </span>
        <span className="eyebrow">YOU’RE IN GOOD COMPANY</span>
        <h1>Welcome to going.</h1>
        <p>Your account is ready. Sign in to find your first great find.</p>
        <button className="button primary" onClick={onLogin}>
          Sign in <ArrowUpRight size={17} />
        </button>
      </section>
    );
  return (
    <>
      <a className="back-link" href="#/">
        <ArrowLeft size={16} />
        Back to Going
      </a>
      <div className="form-layout registration">
        <div className="form-intro">
          <span className="eyebrow">GOOD THINGS START HERE</span>
          <h1>
            A little curiosity.
            <br />
            <em>A great new find.</em>
          </h1>
          <p>Join the bidding, list something special, and be part of its next story.</p>
          <div className="register-points">
            <p>
              <Check size={17} />
              One account to buy and sell
            </p>
            <p>
              <Check size={17} />A heads-up when you’re outbid
            </p>
            <p>
              <Check size={17} />A happy email when you win
            </p>
          </div>
        </div>
        <form className="form-panel" onSubmit={register}>
          <div className="panel-heading">
            <h2>Create your account</h2>
            <span>
              Already here?{' '}
              <button type="button" className="inline-button" onClick={onLogin}>
                Sign in
              </button>
            </span>
          </div>
          <Notice>{error}</Notice>
          <fieldset disabled={busy}>
            <label>
              Username
              <input
                name="username"
                required
                minLength={3}
                maxLength={100}
                pattern="[a-zA-Z0-9._\-]+"
                title="Use letters, numbers, dots, underscores or hyphens"
                autoComplete="username"
                placeholder="Your name on Going"
              />
            </label>
            <label>
              Email address
              <input
                name="email"
                type="email"
                required
                maxLength={254}
                autoComplete="email"
                placeholder="you@example.com"
              />
            </label>
            <label>
              Password
              <input
                name="password"
                type="password"
                required
                minLength={8}
                maxLength={128}
                autoComplete="new-password"
                placeholder="At least 8 characters"
              />
            </label>
            <label>
              Confirm password
              <input
                name="confirm"
                type="password"
                required
                minLength={8}
                maxLength={128}
                autoComplete="new-password"
                placeholder="Once more, just to be sure"
              />
            </label>
            <button className="button primary full" type="submit">
              {busy ? (
                <Spinner label="Creating account…" />
              ) : (
                <>
                  Create account <ArrowUpRight size={17} />
                </>
              )}
            </button>
            <p className="field-help centered">We’ll send auction updates to this email address.</p>
          </fieldset>
        </form>
      </div>
    </>
  );
}

export function Profile({ user, notify, onLogout }) {
  const [confirm, setConfirm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [deleted, setDeleted] = useState(false);
  async function remove(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      await request('/account-service/api/me', { method: 'DELETE' });
      setDeleted(true);
      setConfirm(false);
      notify('Your account has been deleted. Sign out to close this session.');
    } catch (problem) {
      setError(problem.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <section className="page-heading">
        <span className="eyebrow">YOUR CORNER OF GOING</span>
        <h1>Make yourself at home.</h1>
        <p>Your account details and auction updates, in one place.</p>
      </section>
      <div className="profile-layout">
        <section className="form-panel">
          <div className="profile-header">
            <span className="avatar big">{user.username?.[0]?.toUpperCase()}</span>
            <div>
              <h2>{user.username}</h2>
              <p>Ready for your next great find.</p>
            </div>
          </div>
          <dl className="profile-details">
            <div>
              <dt>Username</dt>
              <dd>{user.username}</dd>
            </div>
            <div>
              <dt>Email address</dt>
              <dd>{user.email}</dd>
            </div>
            <div>
              <dt>Account ID</dt>
              <dd className="small mono">{user.userId}</dd>
            </div>
          </dl>
          <button className="button secondary" onClick={onLogout}>
            Sign out
          </button>
        </section>
        <div>
          <section className="tip-card">
            <ShieldCheck size={25} />
            <h3>We’ll keep you in the loop.</h3>
            <p>
              Auction results and outbid notifications are sent to your email. Your password and
              sign-in are managed securely by Keycloak.
            </p>
          </section>
          <section className="danger-card">
            <h3>Close your account</h3>
            <p>
              This permanently removes your account. Existing auctions and bids remain in the
              auction records.
            </p>
            {deleted ? (
              <>
                <Notice tone="success">Your account has been deleted.</Notice>
                <button className="button secondary" onClick={onLogout}>
                  Finish signing out
                </button>
              </>
            ) : (
              <button className="text-button danger" onClick={() => setConfirm(true)}>
                <Trash2 size={15} />
                Delete account
              </button>
            )}
          </section>
        </div>
      </div>
      {confirm && (
        <Dialog title="Delete your account?" onClose={() => setConfirm(false)} busy={busy}>
          <p>
            This permanently deletes your profile and sign-in. Auction records remain, and this
            action cannot be undone.
          </p>
          <form onSubmit={remove}>
            <Notice>{error}</Notice>
            <label className="check-label">
              <input type="checkbox" required disabled={busy} />I understand my account will be
              deleted permanently.
            </label>
            <div className="button-row end">
              <button
                type="button"
                className="button secondary"
                disabled={busy}
                onClick={() => setConfirm(false)}
              >
                Keep account
              </button>
              <button className="button destructive" disabled={busy}>
                {busy ? <Spinner label="Deleting…" /> : 'Delete permanently'}
              </button>
            </div>
          </form>
        </Dialog>
      )}
    </>
  );
}
