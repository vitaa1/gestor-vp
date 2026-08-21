import { HttpClient, HttpHeaders } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { map, Observable, tap } from 'rxjs';
import { UserTimeZoneService } from './user-time-zone.service';

interface AuthenticatedUser {
  username: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly userTimeZone = inject(UserTimeZoneService);
  private readonly authorization = signal<string | null>(null);

  readonly authenticated = computed(() => this.authorization() !== null);

  login(username: string, password: string): Observable<void> {
    const authorization = `Basic ${this.encodeCredentials(username, password)}`;
    return this.http
      .get<AuthenticatedUser>('/api/v1/auth/me', {
        headers: this.requestHeaders(authorization),
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
    return this.requestHeaders(this.authorization());
  }

  private requestHeaders(authorization: string | null): HttpHeaders {
    let headers = new HttpHeaders({ 'X-User-Time-Zone': this.userTimeZone.current() });
    if (authorization) {
      headers = headers.set('Authorization', authorization);
    }
    return headers;
  }

  private encodeCredentials(username: string, password: string): string {
    const bytes = new TextEncoder().encode(`${username}:${password}`);
    let binary = '';
    bytes.forEach((byte) => (binary += String.fromCharCode(byte)));
    return btoa(binary);
  }
}
