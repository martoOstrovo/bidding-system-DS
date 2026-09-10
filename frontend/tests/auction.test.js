import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  cents,
  decimal,
  nextBid,
  ended,
  timeLeft,
  imagePath,
  auctionDuration,
} from '../src/lib/auction.js';

test('offers preserve cents, including amounts beyond JavaScript safe integer precision', () => {
  assert.equal(cents('0.10') + cents('0.20'), 30n);
  assert.equal(decimal('00015.5'), '15.50');
  assert.equal(nextBid('99999999999999998.99'), '99999999999999999.00');
  for (const amount of ['-1', '1.005', '1e3', '', 'NaN', '100000000000000000'])
    assert.throws(() => cents(amount));
});

test('the deadline closes bidding even before the scheduler updates status', () => {
  const now = Date.parse('2030-01-01T10:00:00Z');
  assert.equal(ended({ status: 'OPEN', expirationDate: '2030-01-01T10:00:00Z' }, now), true);
  assert.equal(ended({ status: 'OPEN', expirationDate: '2030-01-01T10:00:01Z' }, now), false);
  assert.equal(ended({ status: 'CLOSED', expirationDate: '2030-01-02T10:00:00Z' }, now), true);
  assert.equal(timeLeft('2030-01-01T10:01:05Z', now), '1m 5s left');
});

test('durations accept a minute and longer without a business maximum', () => {
  for (const duration of [60, 61, 3600, 86400, 315360000])
    assert.equal(auctionDuration(String(duration)), duration);
  for (const duration of ['0', '-1', '59', '60.5', '', 'NaN', 'Infinity', '9007199254740992'])
    assert.throws(() => auctionDuration(duration));
});

test('item images must come from the local image gateway', () => {
  assert.equal(imagePath('/uploads/images/abc-123.png'), '/uploads/images/abc-123.png');
  for (const path of [
    'https://external.test/image.jpg',
    '//external.test/image.jpg',
    '/uploads/images/../secret',
    '/uploads/images/default-item.png',
  ])
    assert.equal(imagePath(path), null);
});
