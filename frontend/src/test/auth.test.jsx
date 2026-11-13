import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../auth/AuthContext.jsx';
import AuthPage from '../pages/AuthPage.jsx';
const resp = (status, body) => ({ ok: status < 400, status, json: async () => body });
const setup = (mode = 'login') => render(<MemoryRouter initialEntries={['/x']}><AuthProvider><Routes>
  <Route path="/x" element={<AuthPage mode={mode} />} /><Route path="/app" element={<p>Dashboard reached</p>} /></Routes></AuthProvider></MemoryRouter>);
beforeEach(() => { localStorage.clear(); global.fetch = vi.fn(); });
describe('login form', () => {
  it('shows validation errors and never calls the API', async () => {
    setup(); await userEvent.click(screen.getByRole('button', { name: 'Log in' }));
    expect(await screen.findByText('Enter a valid email address.')).toBeInTheDocument();
    expect(screen.getByText('Use at least 8 characters.')).toBeInTheDocument(); expect(fetch).not.toHaveBeenCalled();
  });
  it('logs in, stores the refresh token and moves to the app', async () => {
    fetch.mockResolvedValue(resp(200, { accessToken: 'a', refreshToken: 'r' })); setup();
    await userEvent.type(screen.getByLabelText('Email'), 'a@b.com'); await userEvent.type(screen.getByLabelText('Password'), 'password123');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));
    await waitFor(() => expect(screen.getByText('Dashboard reached')).toBeInTheDocument()); expect(localStorage.getItem('dt_refresh')).toBe('r');
  });
  it('shows the server error when credentials are wrong', async () => {
    fetch.mockResolvedValue(resp(401, { message: 'Invalid email or password' })); setup();
    await userEvent.type(screen.getByLabelText('Email'), 'a@b.com'); await userEvent.type(screen.getByLabelText('Password'), 'password123');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
  });
});
