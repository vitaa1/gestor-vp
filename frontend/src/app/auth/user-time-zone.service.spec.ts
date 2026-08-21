import { TestBed } from '@angular/core/testing';
import { UserTimeZoneService } from './user-time-zone.service';

describe('UserTimeZoneService', () => {
  it('should detect the current IANA time zone from the browser', () => {
    const dateTimeFormat = vi.spyOn(Intl, 'DateTimeFormat').mockReturnValue({
      resolvedOptions: () => ({ timeZone: 'America/Recife' }),
    } as Intl.DateTimeFormat);

    const service = TestBed.inject(UserTimeZoneService);

    expect(service.current()).toBe('America/Recife');
    dateTimeFormat.mockRestore();
  });

  it('should fall back to UTC when the browser does not expose a time zone', () => {
    const dateTimeFormat = vi.spyOn(Intl, 'DateTimeFormat').mockReturnValue({
      resolvedOptions: () => ({ timeZone: undefined }),
    } as unknown as Intl.DateTimeFormat);

    const service = TestBed.inject(UserTimeZoneService);

    expect(service.current()).toBe('UTC');
    dateTimeFormat.mockRestore();
  });
});
