import { test, expect } from '@playwright/test';

const user = { userId: 'user-alice', username: 'alice', email: 'alice@example.test' };
const csrf = { headerName: 'X-CSRF-TOKEN', token: 'test-csrf' };
const ids = [
  '11111111-1111-4111-8111-111111111111',
  '22222222-2222-4222-8222-222222222222',
  '33333333-3333-4333-8333-333333333333',
];
const future = () => new Date(Date.now() + 60000).toISOString();

async function mockApi(page, { signedIn = true, conflict = false, failUpload = false } = {}) {
  const calls = [];
  let uploads = 0;
  const auctions = [
    {
      id: ids[0],
      itemId: 'item-1',
      ownerId: 'user-bob',
      highestBidderId: 'user-charlie',
      startingPrice: '40.00',
      currentBid: '65.00',
      expirationDate: future(),
      status: 'OPEN',
    },
    {
      id: ids[1],
      itemId: 'item-2',
      ownerId: user.userId,
      highestBidderId: null,
      startingPrice: '25.00',
      currentBid: '25.00',
      expirationDate: future(),
      status: 'OPEN',
    },
    {
      id: ids[2],
      itemId: 'item-3',
      ownerId: 'user-bob',
      highestBidderId: user.userId,
      startingPrice: '10.00',
      currentBid: '15.00',
      expirationDate: '2020-01-01T00:00:00Z',
      status: 'CLOSED',
    },
  ];
  const items = {
    'item-1': {
      id: 'item-1',
      itemName: 'A classic film camera',
      itemDescription: 'A little analogue magic. Fully working, with its original leather case.',
    },
    'item-2': {
      id: 'item-2',
      itemName: 'Weekend record collection',
      itemDescription: 'A carefully kept selection of jazz records, ready for a new set of ears.',
    },
    'item-3': {
      id: 'item-3',
      itemName: 'The everyday desk lamp',
      itemDescription: 'A warm glow for your favourite reading corner.',
    },
  };
  await page.route(
    /\/(auth|account-service|bidding-service|item-service|uploads)\//,
    async (route) => {
      const req = route.request();
      const path = new URL(req.url()).pathname;
      const method = req.method();
      calls.push({
        path,
        method,
        body: req.postData(),
        headers: req.headers(),
        requestedAt: Date.now(),
      });
      const json = (data, status = 200, headers = {}) =>
        route.fulfill({ status, json: data, headers });
      if (path === '/auth/csrf') return json(csrf);
      if (path === '/auth/login')
        return route.fulfill({ contentType: 'text/html', body: '<h1>Test identity provider</h1>' });
      if (path === '/auth/logout')
        return route.fulfill({ contentType: 'text/html', body: '<h1>Signed out</h1>' });
      if (path === '/account-service/api/me')
        return signedIn ? json(user, method === 'DELETE' ? 200 : 200) : json({}, 401);
      if (path === '/auth/register') return json(user, 201);
      if (path === '/bidding-service/api/list') return json(auctions);
      if (path === '/bidding-service/api/create-with-item') {
        const input = req.postDataJSON();
        auctions.push({
          id: '44444444-4444-4444-8444-444444444444',
          itemId: 'item-4',
          ownerId: user.userId,
          highestBidderId: null,
          currentBid: input.startingPrice,
          startingPrice: input.startingPrice,
          expirationDate: new Date(Date.now() + input.durationSeconds * 1000).toISOString(),
          status: 'OPEN',
        });
        items['item-4'] = { id: 'item-4', ...input.item };
        return json({ statusCode: '201' }, 201, {
          Location: '/bidding-service/api/details/44444444-4444-4444-8444-444444444444',
        });
      }
      if (path.endsWith('/upload-image')) {
        uploads++;
        return failUpload && uploads === 1
          ? json({ detail: 'Upload failed' }, 503)
          : json(items['item-4']);
      }
      const item = path.match(/^\/item-service\/api\/get\/(.+)$/);
      if (item) return json(items[item[1]]);
      const id = path.match(/[a-f0-9-]{36}/i)?.[0];
      const auction = auctions.find((a) => a.id === id);
      if (auction && path.endsWith('/bid')) {
        if (conflict) {
          auction.currentBid = '85.00';
          return json({ detail: 'Offer must be larger than the current bid.' }, 409);
        }
        auction.currentBid = req.postDataJSON().amount;
        auction.highestBidderId = user.userId;
        return json(auction);
      }
      if (auction && method === 'PUT') {
        Object.assign(auction, req.postDataJSON());
        if (req.postDataJSON().durationSeconds !== undefined)
          auction.expirationDate = new Date(
            Date.now() + req.postDataJSON().durationSeconds * 1000,
          ).toISOString();
        auction.currentBid = auction.startingPrice;
        return json({ statusCode: '200' });
      }
      if (auction && method === 'DELETE') {
        auctions.splice(auctions.indexOf(auction), 1);
        return json({ statusCode: '200' });
      }
      if (auction && path.includes('/details/'))
        return json({ ...auction, itemDetails: items[auction.itemId] });
      if (auction && path.includes('/get/')) return json(auction);
      return json({ detail: 'No test fixture for this endpoint' }, 404);
    },
  );
  return calls;
}

test('guest landing, registration validation, and login navigation', async ({ page }) => {
  const calls = await mockApi(page, { signedIn: false });
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Good things. Going, going.' })).toBeVisible();
  await page.screenshot({ path: 'test-results/landing-desktop.png', fullPage: true });
  await page.getByRole('link', { name: 'Get started' }).click();
  await page.getByLabel('Username', { exact: true }).fill('alice');
  await page.getByLabel('Email address', { exact: true }).fill(user.email);
  await page.getByLabel('Password', { exact: true }).fill('example-pass-123');
  await page.getByLabel('Confirm password').fill('different-password');
  await page.getByRole('button', { name: 'Create account', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('don’t match');
  expect(calls.filter((c) => c.path === '/auth/register')).toHaveLength(0);
  await page.getByLabel('Confirm password').fill('example-pass-123');
  await page.getByRole('button', { name: 'Create account', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Welcome to going.' })).toBeVisible();
  expect(calls.find((c) => c.path === '/auth/register').headers['x-csrf-token']).toBe(csrf.token);
  await page.getByRole('button', { name: 'Sign in', exact: true }).last().click();
  await expect(page.getByRole('heading', { name: 'Test identity provider' })).toBeVisible();
});

test('browsing, filters, search, bidding, and ended result', async ({ page }) => {
  const calls = await mockApi(page);
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'A classic film camera' })).toBeVisible();
  await expect(page.locator('.auction-card')).toHaveCount(2);
  await page.screenshot({ path: 'test-results/auctions-desktop.png', fullPage: true });
  await page.getByRole('textbox', { name: 'Search auctions' }).fill('record');
  await expect(page.locator('.auction-card')).toHaveCount(1);
  await page.getByRole('textbox', { name: 'Search auctions' }).fill('');
  await page.getByRole('heading', { name: 'A classic film camera' }).click();
  await page.getByLabel('Your bid').fill('');
  await expect(page.getByLabel('Your bid')).toHaveValue('');
  await page.getByLabel('Your bid').fill('65.00');
  await page.getByRole('button', { name: 'Place bid', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('must be higher');
  expect(calls.filter((c) => c.path.endsWith('/bid'))).toHaveLength(0);
  await page.getByLabel('Your bid').fill('70.25');
  await page.getByRole('button', { name: 'Place bid', exact: true }).click();
  await expect(
    page.getByText('You’re the highest bidder. You can still raise your offer.'),
  ).toBeVisible();
  const offer = calls.find((c) => c.path.endsWith('/bid'));
  expect(JSON.parse(offer.body)).toEqual({ amount: '70.25' });
  expect(offer.headers['x-csrf-token']).toBe(csrf.token);
  await page.screenshot({ path: 'test-results/detail-desktop.png', fullPage: true });
  await page.goto(`/#/auctions/${ids[2]}`);
  await expect(page.getByRole('heading', { name: 'This one’s yours!' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Place bid', exact: true })).toHaveCount(0);
});

test('a competing bid refreshes the amount without silently resubmitting', async ({ page }) => {
  const calls = await mockApi(page, { conflict: true });
  await page.goto(`/#/auctions/${ids[0]}`);
  await page.getByLabel('Your bid').fill('70.00');
  await page.getByRole('button', { name: 'Place bid', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('latest price has been refreshed');
  await expect(page.locator('.current-price strong')).toHaveText('85.00 EUR');
  expect(calls.filter((c) => c.path.endsWith('/bid'))).toHaveLength(1);
});

test('create an auction and recover from a photo failure without duplicating the listing', async ({
  page,
}) => {
  const calls = await mockApi(page, { failUpload: true });
  await page.goto('/#/create');
  await page.getByLabel('Item name', { exact: true }).fill('A reading chair');
  await page
    .getByLabel('Description', { exact: true })
    .fill('A comfortable chair in excellent condition.');
  await page.getByLabel('Starting price').fill('45.50');
  await expect(page.getByLabel('Duration (seconds)')).toHaveValue('60');
  await expect(page.getByLabel('Duration (seconds)')).not.toHaveAttribute('max');
  await page.getByLabel('Duration (seconds)').fill('59');
  await page.getByRole('button', { name: 'Publish listing' }).click();
  expect(calls.filter((c) => c.path.endsWith('/create-with-item'))).toHaveLength(0);
  await page.getByLabel('Duration (seconds)').fill('60');
  const photo = {
    name: 'chair.png',
    mimeType: 'image/png',
    buffer: Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/5MsAAAAASUVORK5CYII=',
      'base64',
    ),
  };
  await page.getByLabel('Item photo', { exact: true }).setInputFiles(photo);
  await page.screenshot({ path: 'test-results/create-desktop.png', fullPage: true });
  await page.getByRole('button', { name: 'Publish listing' }).click();
  await expect(page.getByRole('heading', { name: 'A reading chair', exact: true })).toBeVisible();
  await expect(
    page.getByText('Your listing was created, but the photo could not be added.', { exact: false }),
  ).toBeVisible();
  expect(calls.filter((c) => c.path.endsWith('/create-with-item'))).toHaveLength(1);
  const creation = calls.find((c) => c.path.endsWith('/create-with-item'));
  expect(JSON.parse(creation.body).startingPrice).toBe('45.50');
  expect(JSON.parse(creation.body).durationSeconds).toBe(60);
  expect(JSON.parse(creation.body).expirationDate).toBeUndefined();
  await page.getByRole('button', { name: 'Photo', exact: true }).click();
  await page.getByLabel('Item photo', { exact: true }).setInputFiles(photo);
  await page.getByRole('button', { name: 'Save photo' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect(calls.filter((c) => c.path.endsWith('/upload-image'))).toHaveLength(2);
  expect(calls.find((c) => c.path.endsWith('/upload-image')).headers['content-type']).toContain(
    'multipart/form-data; boundary=',
  );
});

test('owners can edit and delete unbid auctions with explicit confirmation', async ({ page }) => {
  const calls = await mockApi(page);
  await page.goto(`/#/auctions/${ids[1]}`);
  await expect(page.getByRole('button', { name: 'Place bid', exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Edit auction', exact: true }).click();
  await page.getByLabel('Starting price').fill('30.00');
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page.locator('.current-price strong')).toHaveText('30.00 EUR');
  const edit = calls.find((c) => c.method === 'PUT');
  expect(JSON.parse(edit.body).durationSeconds).toBeUndefined();
  expect(JSON.parse(edit.body).expirationDate).toMatch(/Z$/);
  await page.getByRole('button', { name: 'Edit auction', exact: true }).click();
  await page.getByLabel('New duration (seconds)').fill('86400');
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  const extension = calls.filter((c) => c.method === 'PUT').at(-1);
  expect(JSON.parse(extension.body).durationSeconds).toBe(86400);
  expect(JSON.parse(extension.body).expirationDate).toBeUndefined();
  await page.getByRole('button', { name: 'Delete', exact: true }).click();
  expect(calls.filter((c) => c.method === 'DELETE')).toHaveLength(0);
  await page.getByRole('button', { name: 'Delete listing', exact: true }).click();
  await expect(page).toHaveURL(/#\/selling$/);
  await expect(
    page.getByRole('heading', { name: 'Your first listing starts here.' }),
  ).toBeVisible();
});

test('mobile navigation, profile, and CSRF-protected logout form', async ({ page }) => {
  const calls = await mockApi(page);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'A classic film camera' })).toBeVisible();
  await page.screenshot({ path: 'test-results/auctions-mobile.png', fullPage: true });
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
  ).toBeTruthy();
  await page.getByRole('button', { name: 'Open navigation' }).click();
  await page.getByRole('link', { name: 'My bidding', exact: true }).click();
  await page.getByRole('button', { name: 'Ended', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'The everyday desk lamp' })).toBeVisible();
  await page.getByRole('link', { name: 'My account', exact: true }).click();
  await expect(page.getByText(user.email, { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Sign out', exact: true }).last().click();
  await expect(page.getByRole('heading', { name: 'Signed out', exact: true })).toBeVisible();
  expect(calls.find((c) => c.path === '/auth/logout').body).toContain('_csrf=test-csrf');
});
