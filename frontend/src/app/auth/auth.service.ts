import { HttpClient, HttpHeaders } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { map, Observable, tap } from 'rxjs';

interface AuthenticatedUser {
  username: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly authorization = signal<string | null>(null);

  readonly authenticated = computed(() => this.authorization() !== null);

  login(username: string, password: string): Observable<void> {
    const authorization = `Basic ${this.encodeCredentials(username, password)}`;
    return this.http
      .get<AuthenticatedUser>('/api/auth/me', {
        headers: new HttpHeaders({ Authorization: authorization }),
      })
      .pipe(
        tap(() => this.authorization.set(authorization)),
        map(() => undefined),
      );
  }

  logout(): void {
    this.authorization.set(null);
  }

  headers(): HttpHeaders {
    const authorization = this.authorization();
    return authorization ? new HttpHeaders({ Authorization: authorization }) : new HttpHeaders();
  }

  private encodeCredentials(username: string, password: string): string {
    const bytes = new TextEncoder().encode(`${username}:${password}`);
    let binary = '';
    bytes.forEach((byte) => (binary += String.fromCharCode(byte)));
    return btoa(binary);
  }
}
