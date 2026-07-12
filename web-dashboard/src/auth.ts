import type { LoginResponse, User } from './types/api';

const KEY = 'transitops.session';

interface Session {
  accessToken: string;
  refreshToken: string;
  user: User;
}

let current: Session | null = load();

function load(): Session | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

export const session = {
  get user(): User | null {
    return current?.user ?? null;
  },
  get accessToken(): string | null {
    return current?.accessToken ?? null;
  },
  get refreshToken(): string | null {
    return current?.refreshToken ?? null;
  },
  updateTokens(accessToken: string, refreshToken: string): void {
    if (!current) return;
    current = { ...current, accessToken, refreshToken };
    sessionStorage.setItem(KEY, JSON.stringify(current));
  },
  save(res: LoginResponse): void {
    current = { accessToken: res.accessToken, refreshToken: res.refreshToken, user: res.user };
    sessionStorage.setItem(KEY, JSON.stringify(current));
  },
  clear(): void {
    current = null;
    sessionStorage.removeItem(KEY);
  },
};
