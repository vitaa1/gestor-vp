import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { Login } from './login';

describe('Login', () => {
  const authService = {
    login: vi.fn(),
  };

  beforeEach(async () => {
    authService.login.mockReset();
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [{ provide: AuthService, useValue: authService }],
    }).compileComponents();
  });

  it.each([
    [401, 'Usuário ou senha incorretos.'],
    [429, 'Muitas tentativas de login. Aguarde alguns minutos e tente novamente.'],
    [503, 'Não foi possível entrar. Tente novamente.'],
  ])('should explain a login failure with status %i', (status, expectedMessage) => {
    authService.login.mockReturnValue(throwError(() => new HttpErrorResponse({ status })));
    const component = TestBed.createComponent(Login).componentInstance;
    component.form.setValue({ username: 'operador', password: 'senha' });

    component.submit();

    expect(component.errorMessage()).toBe(expectedMessage);
  });
});
