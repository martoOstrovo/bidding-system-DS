import { useCallback, useEffect, useState } from 'react';
import {
  ArrowLeft,
  ArrowUpRight,
  Clock3,
  Info,
  Pencil,
  RefreshCw,
  ShieldCheck,
  Trash2,
  Trophy,
  Upload,
} from 'lucide-react';
import { request } from '../lib/api';
import {
  cents,
  decimal,
  ended,
  auctionDuration,
  MIN_AUCTION_SECONDS,
  money,
  nextBid,
  timeLeft,
} from '../lib/auction';
import { Dialog, ItemImage, Notice, Spinner } from '../components/ui';
import { PhotoInput, uploadPhoto } from './Forms';

export default function AuctionDetail({ id, user, notify, navigate, currency }) {
  const [auction, setAuction] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [error, setError] = useState('');
  const [amount, setAmount] = useState(null);
  const [busy, setBusy] = useState(false);
  const [now, setNow] = useState(Date.now());
  const [dialog, setDialog] = useState(null);
  const [file, setFile] = useState(null);
  const [revision, setRevision] = useState(0);

  const refresh = useCallback(
    async (signal) => {
      try {
        const { data } = await request(`/bidding-service/api/details/${id}`, { signal });
        if (signal?.aborted) return;
        setAuction(data);
        setLoadError('');
      } catch (problem) {
        if (problem.name !== 'AbortError') setLoadError(problem.message);
      } finally {
        if (!signal?.aborted) setLoading(false);
      }
    },
    [id],
  );
  useEffect(() => {
    const controller = new AbortController();
    refresh(controller.signal);
    const poll = setInterval(() => {
      if (!document.hidden && !busy) refresh(controller.signal);
    }, 15000);
    return () => {
      controller.abort();
      clearInterval(poll);
    };
  }, [refresh, busy, revision]);
  useEffect(() => {
    const tick = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(tick);
  }, []);

  if (loading)
    return (
      <div className="page-loading">
        <Spinner label="Taking a closer look…" />
      </div>
    );
  if (!auction)
    return (
      <>
        <a className="back-link" href="#/">
          <ArrowLeft size={16} />
          Back to auctions
        </a>
        <Notice>{loadError}</Notice>
        <button className="button secondary" onClick={() => refresh()}>
          Try again
        </button>
      </>
    );
  const closed = ended(auction, now);
  const own = auction.ownerId === user.userId;
  const leading = auction.highestBidderId === user.userId;
  const hasOffers = !!auction.highestBidderId;
  const minimum = nextBid(auction.currentBid);
  const item = auction.itemDetails;

  async function bid(event) {
    event.preventDefault();
    setError('');
    let offer;
    try {
      offer = decimal(amount ?? minimum);
      if (cents(offer) <= cents(auction.currentBid))
        throw new Error(`Your bid must be higher than ${money(auction.currentBid, currency)}.`);
    } catch (problem) {
      setError(problem.message);
      return;
    }
    setBusy(true);
    try {
      const { data } = await request(`/bidding-service/api/${id}/bid`, {
        method: 'POST',
        body: { amount: offer },
      });
      setAuction((previous) => ({ ...previous, ...data }));
      setAmount(null);
      notify('Bid accepted. You’re in the lead!');
    } catch (problem) {
      setError(
        problem.message +
          (problem.status === 409
            ? ' The latest price has been refreshed. Review it before bidding again.'
            : ''),
      );
      await refresh();
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    setBusy(true);
    setError('');
    try {
      await request(`/bidding-service/api/delete/${id}`, { method: 'DELETE' });
      notify('Your listing has been removed.');
      navigate('/selling');
    } catch (problem) {
      setError(problem.message);
      await refresh();
    } finally {
      setBusy(false);
    }
  }

  async function edit(event) {
    event.preventDefault();
    setError('');
    const fields = new FormData(event.currentTarget);
    let price;
    let timing;
    try {
      price = decimal(fields.get('price'));
      timing =
        fields.get('duration').trim() === ''
          ? { expirationDate: auction.expirationDate }
          : { durationSeconds: auctionDuration(fields.get('duration')) };
    } catch (problem) {
      setError(problem.message);
      return;
    }
    setBusy(true);
    try {
      await request(`/bidding-service/api/put/${id}`, {
        method: 'PUT',
        body: {
          itemId: auction.itemId,
          startingPrice: price,
          ...timing,
        },
      });
      setDialog(null);
      await refresh();
      notify('Listing updated.');
    } catch (problem) {
      setError(problem.message);
    } finally {
      setBusy(false);
    }
  }

  async function addPhoto(event) {
    event.preventDefault();
    if (!file) return;
    setBusy(true);
    setError('');
    try {
      await uploadPhoto(auction.itemId, file);
      setFile(null);
      setDialog(null);
      await refresh();
      notify('Photo updated.');
    } catch (problem) {
      setError(problem.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="detail-top">
        <a className="back-link" href="#/">
          <ArrowLeft size={16} />
          Back to auctions
        </a>
        <button className="text-button" disabled={busy} onClick={() => setRevision((v) => v + 1)}>
          <RefreshCw size={14} />
          Refresh price
        </button>
      </div>
      <Notice>{loadError}</Notice>
      <div className="detail-grid">
        <div>
          <div className="detail-image">
            <ItemImage item={item} large />
            <span className={`badge ${closed ? 'neutral' : 'live'}`}>
              {closed ? (
                'Auction ended'
              ) : (
                <>
                  <span className="live-dot" /> Live auction
                </>
              )}
            </span>
          </div>
          <section className="description">
            <span className="eyebrow">THE DETAILS</span>
            <h2>A closer look</h2>
            <p>{item?.itemDescription || 'Item information is currently unavailable.'}</p>
            <span className="listing-id">Listing {id}</span>
          </section>
        </div>
        <section className="bid-panel">
          <span className="eyebrow">{own ? 'YOUR LISTING' : 'SOMETHING WORTH BIDDING FOR'}</span>
          <h1>{item?.itemName || 'Auction details'}</h1>
          <div className="detail-clock">
            <Clock3 size={17} />
            <strong>{closed ? 'Auction ended' : timeLeft(auction.expirationDate, now)}</strong>
            <span>
              {new Date(auction.expirationDate).toLocaleString(undefined, {
                dateStyle: 'medium',
                timeStyle: 'short',
              })}
            </span>
          </div>
          <div className="current-price">
            <span>
              {closed
                ? hasOffers
                  ? 'Final bid'
                  : 'Starting price'
                : hasOffers
                  ? 'Current bid'
                  : 'Starting price'}
            </span>
            <strong>{money(auction.currentBid, currency)}</strong>
            <span>
              {hasOffers
                ? `Started at ${money(auction.startingPrice, currency)}`
                : 'No bids yet. Every good story needs a beginning.'}
            </span>
          </div>
          {closed ? (
            <div className="result-box">
              <Trophy size={25} />
              <div>
                <h3>
                  {!hasOffers
                    ? 'Ended without a bid'
                    : auction.status !== 'CLOSED'
                      ? 'Bidding has closed'
                      : leading
                        ? 'This one’s yours!'
                        : own
                          ? 'Your auction has a winner.'
                          : 'This one found a new home.'}
                </h3>
                <p>
                  {auction.status !== 'CLOSED'
                    ? 'The auction result is being finalized. We’ll email the owner and winner.'
                    : hasOffers
                      ? leading
                        ? 'You won this auction. Watch your inbox for the result.'
                        : own
                          ? 'The winning amount is shown above. Watch your inbox for the result.'
                          : 'There are more good things waiting on the block.'
                      : 'No winning bidder was selected.'}
                </p>
              </div>
            </div>
          ) : own ? (
            <Notice tone="info">
              This is your listing. You can follow the bidding here; owners cannot bid on their own
              items.
            </Notice>
          ) : (
            <form className="bid-form" onSubmit={bid}>
              {leading && (
                <Notice tone="success">
                  You’re the highest bidder. You can still raise your offer.
                </Notice>
              )}
              <Notice>{error}</Notice>
              <label htmlFor="offer">
                Your bid <span className="label-note">({currency})</span>
              </label>
              <input
                id="offer"
                name="amount"
                inputMode="decimal"
                required
                value={amount ?? minimum}
                onChange={(e) => setAmount(e.target.value)}
                disabled={busy || !!loadError}
                pattern="[0-9]{1,17}(\.[0-9]{1,2})?"
              />
              <p className="field-help">Enter {money(minimum, currency)} or more.</p>
              <button className="button primary full" disabled={busy || !!loadError}>
                {busy ? (
                  <Spinner label="Placing your bid…" />
                ) : (
                  <>
                    Place bid <ArrowUpRight size={18} />
                  </>
                )}
              </button>
              <p className="bid-note">
                <ShieldCheck size={15} />
                We’ll email you if you’re outbid or if you win.
              </p>
            </form>
          )}
          {own && !hasOffers && (
            <div className="owner-actions">
              <h3>Manage your listing</h3>
              <div className="button-row">
                {!closed && (
                  <>
                    <button
                      className="button secondary compact"
                      onClick={() => {
                        setError('');
                        setDialog('edit');
                      }}
                    >
                      <Pencil size={15} />
                      Edit auction
                    </button>
                    <button
                      className="button secondary compact"
                      onClick={() => {
                        setError('');
                        setDialog('photo');
                      }}
                    >
                      <Upload size={15} />
                      Photo
                    </button>
                  </>
                )}
                <button
                  className="text-button danger"
                  onClick={() => {
                    setError('');
                    setDialog('delete');
                  }}
                >
                  <Trash2 size={15} />
                  Delete
                </button>
              </div>
            </div>
          )}
          {own && hasOffers && (
            <p className="bid-note">
              <Info size={15} />
              Listings with bids can no longer be edited or deleted.
            </p>
          )}
        </section>
      </div>
      {dialog && (
        <Dialog
          title={
            dialog === 'edit'
              ? 'Edit your auction'
              : dialog === 'photo'
                ? 'Add an item photo'
                : 'Delete this listing?'
          }
          onClose={() => {
            setDialog(null);
            setError('');
          }}
          busy={busy}
        >
          <Notice>{error}</Notice>
          {dialog === 'delete' ? (
            <>
              <p>Your listing will be removed from the auctions. This cannot be undone.</p>
              <div className="button-row end">
                <button
                  className="button secondary"
                  disabled={busy}
                  onClick={() => setDialog(null)}
                >
                  Keep listing
                </button>
                <button className="button destructive" disabled={busy} onClick={remove}>
                  {busy ? <Spinner label="Deleting…" /> : 'Delete listing'}
                </button>
              </div>
            </>
          ) : dialog === 'edit' ? (
            <form onSubmit={edit}>
              <fieldset disabled={busy}>
                <label>
                  Starting price ({currency})
                  <input
                    name="price"
                    required
                    inputMode="decimal"
                    defaultValue={auction.startingPrice}
                  />
                </label>
                <label>
                  New duration (seconds)
                  <input
                    name="duration"
                    type="number"
                    placeholder="Keep current deadline"
                    min={MIN_AUCTION_SECONDS}
                    step={1}
                  />
                </label>
                <p className="field-help">
                  Leave blank to keep the deadline, or enter at least 60 seconds from saving. There
                  is no maximum duration.
                </p>
                <button className="button primary full">
                  {busy ? <Spinner label="Saving…" /> : 'Save changes'}
                </button>
              </fieldset>
            </form>
          ) : (
            <form onSubmit={addPhoto}>
              <PhotoInput file={file} onChange={setFile} disabled={busy} />
              <button className="button primary full" disabled={!file || busy}>
                {busy ? <Spinner label="Uploading…" /> : 'Save photo'}
              </button>
            </form>
          )}
        </Dialog>
      )}
    </>
  );
}
