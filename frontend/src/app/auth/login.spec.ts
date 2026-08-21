import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { NEVER, throwError } from 'rxjs';
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

  it('should show the gestorVP identity', () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.login-card__mark')?.textContent).toBe('gVP');
    expect(compiled.querySelector('.eyebrow')?.textContent).toBe('gestorVP');
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

  it('should let the user show and hide the typed password', () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    const passwordInput = fixture.nativeElement.querySelector('#password') as HTMLInputElement;
    const toggleButton = fixture.nativeElement.querySelector(
      '[aria-label="Mostrar senha"]',
    ) as HTMLButtonElement;

    expect(passwordInput.type).toBe('password');
    expect(toggleButton.type).toBe('button');
    expect(toggleButton.getAttribute('aria-pressed')).toBe('false');

    toggleButton.click();
    fixture.detectChanges();

    expect(passwordInput.type).toBe('text');
    expect(toggleButton.getAttribute('aria-label')).toBe('Ocultar senha');
    expect(toggleButton.getAttribute('aria-pressed')).toBe('true');

    toggleButton.click();
    fixture.detectChanges();

    expect(passwordInput.type).toBe('password');
  });

  it('should mask a visible password when login starts', () => {
    authService.login.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(Login);
    const component = fixture.componentInstance;
    component.form.setValue({ username: 'operador', password: 'senha' });
    component.togglePasswordVisibility();
    fixture.detectChanges();

    component.submit();
    fixture.detectChanges();

    const passwordInput = fixture.nativeElement.querySelector('#password') as HTMLInputElement;
    const toggleButton = fixture.nativeElement.querySelector(
      '.password-control__toggle',
    ) as HTMLButtonElement;
    expect(passwordInput.type).toBe('password');
    expect(component.passwordVisible()).toBe(false);
    expect(toggleButton.disabled).toBe(true);

    toggleButton.click();
    fixture.detectChanges();

    expect(passwordInput.type).toBe('password');
  });
});
