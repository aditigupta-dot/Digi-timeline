import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api, setTokens } from '../api.js';
const resp = (status, body) => ({ ok: status < 400, status, json: async () => body });
beforeEach(() => { localStorage.clear(); global.fetch = vi.fn(); });
describe('api layer', () => {
  it('refreshes once after a 401 and retries with the new token', async () => {
    setTokens({ accessToken: 'old', refreshToken: 'r1' });
    fetch.mockResolvedValueOnce(resp(401, { message: 'expired' })).mockResolvedValueOnce(resp(200, { accessToken: 'new', refreshToken: 'r2' })).mockResolvedValueOnce(resp(200, { ok: 1 }));
    expect(await api('/tasks')).toEqual({ ok: 1 }); expect(fetch).toHaveBeenCalledTimes(3);
    expect(fetch.mock.calls[2][1].headers.Authorization).toBe('Bearer new');
  });
  it('rejects when the refresh also fails', async () => {
    setTokens({ accessToken: 'old', refreshToken: 'r1' });
    fetch.mockResolvedValue(resp(401, { message: 'Session expired' })); await expect(api('/tasks')).rejects.toThrow('Session expired');
  });
  it('surfaces the server message for other errors', async () => {
    fetch.mockResolvedValue(resp(400, { message: 'title must not be blank' })); await expect(api('/tasks', { method: 'POST', body: {} })).rejects.toThrow('title must not be blank');
  });
});
