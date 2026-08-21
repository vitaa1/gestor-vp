import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../auth/auth.service';
import { StockEntryService } from './stock-entry.service';

class AuthServiceStub {
  headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: 'Basic test-credentials' });
  }
}

describe('StockEntryService', () => {
  let service: StockEntryService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        StockEntryService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useClass: AuthServiceStub },
      ],
    });
    service = TestBed.inject(StockEntryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the next active-stock slice with its complete cursor', () => {
    service.list(50, '2030-01-02', '2026-08-22T00:30:00Z', 42).subscribe();

    const request = http.expectOne(
      (candidate) =>
        candidate.url === '/api/v1/stock-entries' &&
        candidate.params.get('size') === '50' &&
        candidate.params.get('cursorExpirationDate') === '2030-01-02' &&
        candidate.params.get('cursorCreatedAt') === '2026-08-22T00:30:00Z' &&
        candidate.params.get('cursorId') === '42',
    );
    expect(request.request.headers.get('Authorization')).toBe('Basic test-credentials');
    request.flush({
      content: [],
      size: 50,
      hasNext: false,
      nextCursorExpirationDate: null,
      nextCursorCreatedAt: null,
      nextCursorId: null,
    });
  });

  it('searches products with name, status and a stable continuation cursor', () => {
    service.search('  pão  ', 'WATCH', 20, '2030-01-02', '2026-08-22T00:30:00Z', 42).subscribe();

    const request = http.expectOne(
      (candidate) =>
        candidate.url === '/api/v1/stock-entries' &&
        candidate.params.get('query') === 'pão' &&
        candidate.params.get('status') === 'WATCH' &&
        candidate.params.get('size') === '20' &&
        candidate.params.get('cursorExpirationDate') === '2030-01-02' &&
        candidate.params.get('cursorCreatedAt') === '2026-08-22T00:30:00Z' &&
        candidate.params.get('cursorId') === '42',
    );
    expect(request.request.headers.get('Authorization')).toBe('Basic test-credentials');
    request.flush({
      content: [],
      size: 20,
      hasNext: false,
      nextCursorExpirationDate: null,
      nextCursorCreatedAt: null,
      nextCursorId: null,
    });
  });
});
