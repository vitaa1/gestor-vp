import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../auth/auth.service';
import { StockMovementService } from './stock-movement.service';

class AuthServiceStub {
  headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: 'Basic test-credentials' });
  }
}

describe('StockMovementService', () => {
  let service: StockMovementService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        StockMovementService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useClass: AuthServiceStub },
      ],
    });
    service = TestBed.inject(StockMovementService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the next history slice with its cursor and the authentication header', () => {
    service.list(20, '2026-08-15T18:30:00Z', 42).subscribe();

    const request = http.expectOne(
      (candidate) =>
        candidate.url === '/api/v1/stock-movements' &&
        candidate.params.get('size') === '20' &&
        candidate.params.get('cursorCreatedAt') === '2026-08-15T18:30:00Z' &&
        candidate.params.get('cursorId') === '42',
    );
    expect(request.request.headers.get('Authorization')).toBe('Basic test-credentials');
    request.flush({
      content: [],
      size: 20,
      hasNext: false,
      nextCursorCreatedAt: null,
      nextCursorId: null,
    });
  });
});
