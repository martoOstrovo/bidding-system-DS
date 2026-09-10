export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message);
    this.status = status;
  }
}

function messageFor(body, status) {
  if (status === 401) return 'Your session has ended. Please sign in again.';
  if (status === 429) return 'A few too many requests. Wait a moment, then try again.';
  if (status === 502 || status === 503 || status === 504)
    return 'The service is temporarily unavailable. Please try again shortly.';
  return (
    body?.detail ||
    body?.errorMessage ||
    body?.message ||
    (body &&
      !body.status &&
      Object.values(body)
        .filter((v) => typeof v === 'string')
        .join(' ')) ||
    `The request could not be completed (${status}).`
  );
}

export async function request(path, { method = 'GET', body, signal, anonymous = false } = {}) {
  const headers = { Accept: 'application/json' };
  if (method !== 'GET') {
    const { data: token } = await request('/auth/csrf', { signal, anonymous: true });
    if (!token?.headerName || !token?.token)
      throw new ApiError('Unable to secure this request. Refresh the page and try again.');
    headers[token.headerName] = token.token;
  }
  const multipart = body instanceof FormData;
  if (body && !multipart) headers['Content-Type'] = 'application/json';
  let response;
  try {
    response = await fetch(path, {
      method,
      headers,
      credentials: 'same-origin',
      cache: 'no-store',
      body: body ? (multipart ? body : JSON.stringify(body)) : undefined,
      signal: signal
        ? AbortSignal.any([signal, AbortSignal.timeout(25000)])
        : AbortSignal.timeout(25000),
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new ApiError('Unable to reach the service. Check your connection and try again.');
  }
  const text = await response.text();
  let data = null;
  try {
    // Preserve the exact decimal source on modern browsers, including large auction amounts.
    data = text
      ? JSON.parse(text, (_key, value, context) =>
          typeof value === 'number' ? (context?.source ?? String(value)) : value,
        )
      : null;
  } catch {
    /* A proxy can return a plain-text error instead of JSON. */
  }
  if (!response.ok) {
    if (response.status === 401 && !anonymous) window.dispatchEvent(new Event('session-expired'));
    throw new ApiError(messageFor(data, response.status), response.status);
  }
  if (text && data === null)
    throw new ApiError('The service returned an unexpected response. Please try again.');
  return { data, location: response.headers.get('Location') };
}

export async function logout() {
  const { data } = await request('/auth/csrf', { anonymous: true });
  if (!data?.token) throw new ApiError('Unable to sign out. Refresh the page and try again.');
  const form = document.createElement('form');
  form.method = 'POST';
  form.action = '/auth/logout';
  const input = document.createElement('input');
  input.type = 'hidden';
  input.name = '_csrf';
  input.value = data.token;
  form.append(input);
  document.body.append(form);
  form.submit();
}
