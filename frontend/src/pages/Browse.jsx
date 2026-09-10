import { useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowUpRight,
  Clock3,
  Plus,
  Search,
  SlidersHorizontal,
  RefreshCw,
  ArrowRight,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { request } from '../lib/api';
import { ended, money, timeLeft } from '../lib/auction';
import { AuctionArt, Empty, ItemImage, Notice, Spinner } from '../components/ui';

export default function Browse({ user, view, currency }) {
  const [auctions, setAuctions] = useState([]);
  const [items, setItems] = useState({});
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('active');
  const [sort, setSort] = useState('ending');
  const [page, setPage] = useState(1);
  const [version, setVersion] = useState(0);
  const [now, setNow] = useState(Date.now());
  const cache = useRef({});
  const [hydrating, setHydrating] = useState(false);
  const [missingItems, setMissingItems] = useState(false);

  useEffect(() => {
    const tick = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(tick);
  }, []);
  useEffect(() => {
    const controller = new AbortController();
    let running = false;
    async function load() {
      if (running || document.hidden) return;
      running = true;
      setRefreshing(true);
      try {
        const { data } = await request('/bidding-service/api/list', { signal: controller.signal });
        if (controller.signal.aborted) return;
        setAuctions(data);
        setError('');
        setLoading(false);
        const ids = [...new Set(data.map((a) => a.itemId))].filter((id) => !cache.current[id]);
        setHydrating(ids.length > 0);
        let failed = false;
        // Bound detail traffic. Successful item metadata is cached while this page is open.
        for (let i = 0; i < ids.length && !controller.signal.aborted; i += 4) {
          await Promise.all(
            ids.slice(i, i + 4).map(async (id) => {
              try {
                cache.current[id] = (
                  await request(`/item-service/api/get/${id}`, { signal: controller.signal })
                ).data;
              } catch (problem) {
                if (problem.name !== 'AbortError') failed = true;
              }
            }),
          );
          if (!controller.signal.aborted) setItems({ ...cache.current });
          if (i + 4 < ids.length) await new Promise((resolve) => setTimeout(resolve, 600));
        }
        if (!controller.signal.aborted) {
          setMissingItems(failed);
          setHydrating(false);
        }
      } catch (problem) {
        if (problem.name !== 'AbortError') setError(problem.message);
      } finally {
        running = false;
        if (!controller.signal.aborted) {
          setLoading(false);
          setRefreshing(false);
        }
      }
    }
    load();
    const interval = setInterval(load, 20000);
    document.addEventListener('visibilitychange', load);
    return () => {
      controller.abort();
      clearInterval(interval);
      document.removeEventListener('visibilitychange', load);
    };
  }, [version]);

  useEffect(() => setPage(1), [query, filter, sort]);
  const owned = auctions.filter((a) => a.ownerId === user.userId);
  const leading = auctions.filter((a) => a.highestBidderId === user.userId);
  const source = view === 'selling' ? owned : view === 'bidding' ? leading : auctions;
  const filtered = useMemo(
    () =>
      source
        .filter((a) => {
          const closed = ended(a, now);
          return (
            (filter === 'all' || (filter === 'active' ? !closed : closed)) &&
            `${items[a.itemId]?.itemName || ''} ${items[a.itemId]?.itemDescription || ''} ${a.id}`
              .toLowerCase()
              .includes(query.toLowerCase().trim())
          );
        })
        .sort((a, b) =>
          sort === 'price-low'
            ? Number(a.currentBid) - Number(b.currentBid)
            : sort === 'price-high'
              ? Number(b.currentBid) - Number(a.currentBid)
              : new Date(a.expirationDate) - new Date(b.expirationDate),
        ),
    [source, items, filter, now, query, sort],
  );
  const pageCount = Math.max(1, Math.ceil(filtered.length / 12));
  const visiblePage = Math.min(page, pageCount);
  const visible = filtered.slice((visiblePage - 1) * 12, visiblePage * 12);
  const activeCount = auctions.filter((a) => !ended(a, now)).length;

  return (
    <>
      {view === 'explore' ? (
        <section className="hero">
          <div className="hero-copy">
            <span className="eyebrow">
              <span className="live-dot" /> A GOOD DAY FOR A GREAT FIND
            </span>
            <h1>
              Find your next
              <br />
              <em>something special.</em>
            </h1>
            <p>
              Discover things with a story. Place your bid.
              <br className="desktop-break" /> Give them a whole new chapter.
            </p>
            <a href="#/create" className="button dark">
              Have something to sell? <ArrowUpRight size={17} />
            </a>
          </div>
          <AuctionArt />
        </section>
      ) : (
        <section className="page-heading">
          <span className="eyebrow">YOUR CORNER OF GOING</span>
          <h1>{view === 'selling' ? 'My listings.' : 'My bidding.'}</h1>
          <p>
            {view === 'selling'
              ? 'The things you’re sending on to their next chapter.'
              : 'Auctions you currently lead, and the ones you’ve won.'}
          </p>
          {view === 'bidding' && (
            <p className="small muted">
              Past offers are not stored. Auctions you’ve been outbid on won’t appear here.
            </p>
          )}
        </section>
      )}
      <div className="market-strip">
        <span>
          <span className="live-dot" />
          <strong>{activeCount}</strong> live auctions
        </span>
        <span>
          <strong>{owned.length}</strong> your listings
        </span>
        <span>
          <strong>{leading.filter((a) => !ended(a, now)).length}</strong> currently leading
        </span>
        <span className="strip-note">
          <Clock3 size={14} /> Fresh prices every 20 seconds
        </span>
      </div>
      <section className="market-section" aria-labelledby="market-title">
        <div className="section-heading">
          <div>
            <span className="eyebrow">
              {view === 'explore' ? 'TAKE A LOOK AROUND' : 'ALL IN ONE PLACE'}
            </span>
            <h2 id="market-title">
              {view === 'explore'
                ? 'On the block'
                : view === 'selling'
                  ? 'Your auctions'
                  : 'Your next finds'}
              <span className="count">{filtered.length}</span>
            </h2>
          </div>
          <button
            className="text-button"
            disabled={refreshing}
            onClick={() => {
              cache.current = {};
              setVersion((v) => v + 1);
            }}
          >
            <RefreshCw size={15} className={refreshing ? 'spin' : ''} />
            Refresh
          </button>
        </div>
        <div className="browse-toolbar">
          <div className="tabs" aria-label="Auction status">
            {[
              ['active', 'Live now'],
              ['ended', 'Ended'],
              ['all', 'All auctions'],
            ].map(([value, label]) => (
              <button
                key={value}
                className={filter === value ? 'selected' : ''}
                aria-pressed={filter === value}
                onClick={() => setFilter(value)}
              >
                {label}
              </button>
            ))}
          </div>
          <div className="browse-controls">
            <label className="search-field">
              <Search size={17} />
              <input
                aria-label="Search auctions"
                placeholder="Find something good…"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </label>
            <label className="sort-field">
              <SlidersHorizontal size={16} />
              <select
                aria-label="Sort auctions"
                value={sort}
                onChange={(e) => setSort(e.target.value)}
              >
                <option value="ending">Ending soon</option>
                <option value="price-low">Price: low to high</option>
                <option value="price-high">Price: high to low</option>
              </select>
            </label>
          </div>
        </div>
        <Notice>{error}</Notice>
        {missingItems && (
          <Notice tone="info">
            Some item details couldn’t be loaded. Refresh to try again; you can still open the
            auctions.
          </Notice>
        )}
        {hydrating && (
          <div className="hydrating">
            <Spinner label="Loading item details…" />
          </div>
        )}
        {loading ? (
          <div className="auction-grid" aria-label="Loading auctions">
            {Array.from({ length: 6 }, (_, i) => (
              <div key={i} className="skeleton-card">
                <div />
                <span />
                <span />
              </div>
            ))}
          </div>
        ) : visible.length ? (
          <div className="auction-grid">
            {visible.map((auction) => {
              const closed = ended(auction, now);
              const mine = auction.ownerId === user.userId;
              const winning = auction.highestBidderId === user.userId;
              return (
                <a href={`#/auctions/${auction.id}`} className="auction-card" key={auction.id}>
                  <div className="card-media">
                    <ItemImage item={items[auction.itemId]} />
                    <span className={`badge ${closed ? 'neutral' : 'live'}`}>
                      {closed ? (
                        'Ended'
                      ) : (
                        <>
                          <span className="live-dot" /> Live auction
                        </>
                      )}
                    </span>
                    {(mine || winning) && (
                      <span className="personal-badge">
                        {mine
                          ? 'Your listing'
                          : closed && auction.status === 'CLOSED'
                            ? 'You won'
                            : closed
                              ? 'Awaiting result'
                              : 'You’re leading'}
                      </span>
                    )}
                    <span className="card-arrow">
                      <ArrowUpRight size={18} />
                    </span>
                  </div>
                  <div className="card-body">
                    <h3>
                      {items[auction.itemId]?.itemName || `Auction ${auction.id.slice(0, 8)}`}
                    </h3>
                    <p>
                      {items[auction.itemId]?.itemDescription ||
                        'Open this auction for more details.'}
                    </p>
                    <div className="card-bottom">
                      <div>
                        <span>
                          {closed
                            ? auction.highestBidderId
                              ? 'Final bid'
                              : 'Starting price'
                            : auction.highestBidderId
                              ? 'Current bid'
                              : 'Starting price'}
                        </span>
                        <strong>{money(auction.currentBid, currency)}</strong>
                      </div>
                      <div
                        className={`time-left ${!closed && new Date(auction.expirationDate) - now < 3600000 ? 'urgent' : ''}`}
                      >
                        <Clock3 size={14} />
                        {closed ? 'Auction ended' : timeLeft(auction.expirationDate, now)}
                      </div>
                    </div>
                  </div>
                </a>
              );
            })}
          </div>
        ) : (
          !error && (
            <Empty
              title={
                query
                  ? 'No finds this time.'
                  : view === 'selling'
                    ? 'Your first listing starts here.'
                    : view === 'bidding'
                      ? 'Make your first move.'
                      : 'A little quiet on the block.'
              }
              action={
                view === 'selling' || view === 'explore' ? (
                  <a href="#/create" className="button primary">
                    <Plus size={16} />
                    List an item
                  </a>
                ) : (
                  <a href="#/" className="button primary">
                    Explore auctions <ArrowRight size={16} />
                  </a>
                )
              }
            >
              {query
                ? 'Try another name or a different filter.'
                : filter === 'ended'
                  ? 'No ended auctions to show yet.'
                  : 'Check back soon, or start something with a listing of your own.'}
            </Empty>
          )
        )}
        {pageCount > 1 && (
          <div className="pagination">
            <button
              className="button secondary compact"
              disabled={visiblePage === 1}
              onClick={() => setPage(visiblePage - 1)}
            >
              <ChevronLeft size={16} />
              Previous
            </button>
            <span>
              Page {visiblePage} of {pageCount}
            </span>
            <button
              className="button secondary compact"
              disabled={visiblePage === pageCount}
              onClick={() => setPage(visiblePage + 1)}
            >
              Next
              <ChevronRight size={16} />
            </button>
          </div>
        )}
      </section>
    </>
  );
}
