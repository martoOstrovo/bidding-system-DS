export function cents(value) {
  const text = String(value ?? '').trim();
  if (!/^\d{1,17}(\.\d{1,2})?$/.test(text))
    throw new Error('Enter an amount with up to 17 digits and two decimal places.');
  const [whole, fraction = ''] = text.split('.');
  return BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0'));
}

export function decimal(value) {
  const amount = cents(value);
  return `${amount / 100n}.${String(amount % 100n).padStart(2, '0')}`;
}

export function nextBid(value) {
  const amount = cents(value) + 1n;
  return `${amount / 100n}.${String(amount % 100n).padStart(2, '0')}`;
}

export function money(value, currency = 'EUR') {
  try {
    const amount = cents(value);
    return `${(amount / 100n).toLocaleString('en-US')}.${String(amount % 100n).padStart(2, '0')} ${currency}`;
  } catch {
    return 'Amount unavailable';
  }
}

export function ended(auction, now = Date.now()) {
  return auction.status === 'CLOSED' || new Date(auction.expirationDate).getTime() <= now;
}

export function timeLeft(date, now = Date.now()) {
  const seconds = Math.max(0, Math.ceil((new Date(date).getTime() - now) / 1000));
  if (!seconds) return 'Auction ended';
  if (seconds >= 86400)
    return `${Math.floor(seconds / 86400)}d ${Math.floor((seconds % 86400) / 3600)}h left`;
  if (seconds >= 3600)
    return `${Math.floor(seconds / 3600)}h ${Math.floor((seconds % 3600) / 60)}m left`;
  if (seconds >= 60) return `${Math.floor(seconds / 60)}m ${seconds % 60}s left`;
  return `${seconds}s left`;
}

export const MIN_AUCTION_SECONDS = 60;

export function auctionDuration(value) {
  const seconds = Number(value);
  if (!Number.isSafeInteger(seconds) || seconds < MIN_AUCTION_SECONDS) {
    throw new Error('Enter a duration of at least 60 whole seconds.');
  }
  return seconds;
}

export function imagePath(path) {
  // Only render images served by our gateway, never arbitrary URLs from item metadata.
  return typeof path === 'string' &&
    /^\/uploads\/images\/[a-zA-Z0-9._-]+$/.test(path) &&
    !path.endsWith('/default-item.png')
    ? path
    : null;
}
