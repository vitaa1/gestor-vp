import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);

  readonly loggedIn = output<void>();
  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly form = this.formBuilder.group({
    username: ['', [Validators.required, Validators.maxLength(120)]],
    password: ['', [Validators.required, Validators.maxLength(200)]],
  });

  submit(): void {
    this.errorMessage.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { username, password } = this.form.getRawValue();
    this.submitting.set(true);
    this.authService
      .login(username.trim(), password)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          this.form.reset();
          this.loggedIn.emit();
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.loginErrorMessage(error.status));
        },
      });
  }

  private loginErrorMessage(status: number): string {
    if (status === 401) {
      return 'Usuário ou senha incorretos.';
    }
    if (status === 429) {
      return 'Muitas tentativas de login. Aguarde alguns minutos e tente novamente.';
    }
    return 'Não foi possível entrar. Tente novamente.';
  }
}
