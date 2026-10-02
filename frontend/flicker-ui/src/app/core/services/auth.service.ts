import { Injectable, signal, inject, PLATFORM_ID, computed } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { Router } from '@angular/router';

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  username: string;
  email: string;
  avatarUrl: string | null;
  role: string;
  expiresAt: string;
}

export interface MessageResponse {
  message: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = 'http://localhost:8081/api/v1/auth';
  private platformId = inject(PLATFORM_ID);
  private isBrowser = isPlatformBrowser(this.platformId);

  token = signal<string | null>(this.getStoredItem('token'));
  currentUser = signal<string | null>(this.getStoredItem('username'));
  avatarUrl = signal<string | null>(this.resolveMediaUrl(this.getStoredItem('avatarUrl')));
  isAuthenticated = computed(() => !!this.token() && !!this.currentUser());

  constructor(private http: HttpClient, private router: Router) {
    const token = this.token();
    if (token && !this.isTokenValid(token)) this.clearSession();
  }

  private isTokenValid(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return typeof payload.exp !== 'number' || payload.exp * 1000 > Date.now();
    } catch { return false; }
  }

  private storeSession(token: string, username: string): void {
    if (this.isBrowser) { localStorage.setItem('token', token); localStorage.setItem('username', username); }
    this.token.set(token); this.currentUser.set(username);
  }

  clearSession(): void {
    if (this.isBrowser) { localStorage.removeItem('token'); localStorage.removeItem('username'); localStorage.removeItem('avatarUrl'); }
    this.token.set(null); this.currentUser.set(null); this.avatarUrl.set(null);
  }

  private getStoredItem(key: string): string | null {
    if (this.isBrowser && typeof window !== 'undefined') {
      return localStorage.getItem(key);
    }
    return null;
  }

  resolveMediaUrl(url: string | null | undefined): string | null {
    if (!url) return null;
    try {
      return new URL(url, 'http://localhost:8081').toString();
    } catch {
      return null;
    }
  }

  register(payload: { username: string; email: string; password: string }) {
    return this.http.post<MessageResponse>(`${this.apiUrl}/register`, payload);
  }

  login(credentials: { username: string; password: string }) {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(res => {
        this.storeSession(res.accessToken, res.username || credentials.username);
        this.setAvatarUrl(res.avatarUrl || null);
      })
    );
  }

  logout(): void {
    this.clearSession();
    this.router.navigate(['/login']);
  }

  setAvatarUrl(url: string | null) {
    const resolvedUrl = this.resolveMediaUrl(url);
    if (this.isBrowser) {
      if (resolvedUrl) localStorage.setItem('avatarUrl', resolvedUrl);
      else localStorage.removeItem('avatarUrl');
    }
    this.avatarUrl.set(resolvedUrl);
  }
}
