import { Injectable, signal } from '@angular/core';

export type UserInfo = { id: number; email?: string; fullName?: string; role: string };

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private _token = signal<string | null>(localStorage.getItem('token'));
  private _user = signal<UserInfo | null>(localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')!) : null);

  token = this._token.asReadonly();
  user = this._user.asReadonly();

  isAuthenticated() {
    return !!this._token();
  }

  setSession(token: string, user: UserInfo) {
    this._token.set(token);
    this._user.set(user);
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
  }

  logout() {
    this._token.set(null);
    this._user.set(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }
}
