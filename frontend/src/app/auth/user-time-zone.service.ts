import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class UserTimeZoneService {
  current(): string {
    try {
      return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
    } catch {
      return 'UTC';
    }
  }
}
