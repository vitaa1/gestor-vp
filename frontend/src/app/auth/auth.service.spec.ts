import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { UserTimeZoneService } from './user-time-zone.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: UserTimeZoneService,
          useValue: { current: () => 'America/Manaus' },
        },
      ],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should authenticate in memory with a Basic authorization header', () => {
    service.login('operador', 'senha').subscribe();

    const request = http.expectOne('/api/v1/auth/me');
    expect(request.request.headers.get('Authorization')).toBe('Basic b3BlcmFkb3I6c2VuaGE=');
    expect(request.request.headers.get('X-User-Time-Zone')).toBe('America/Manaus');
    request.flush({ username: 'operador' });

    expect(service.authenticated()).toBe(true);
    expect(service.headers().get('X-User-Time-Zone')).toBe('America/Manaus');
    service.logout();
    expect(service.authenticated()).toBe(false);
  });
});
