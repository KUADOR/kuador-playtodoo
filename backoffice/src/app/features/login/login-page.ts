import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { asApiError } from '../../core/api/api-error';
import { COMMON_MESSAGES } from '../../core/api/error-messages';
import { HOME_URL } from '../../core/app-paths';
import { Session } from '../../core/auth/session';
import { Alert } from '../../shared/ui/alert/alert';
import { Button } from '../../shared/ui/button/button';
import { Field } from '../../shared/ui/field/field';

/** Texts of P1 «Iniciar sesión» («Pantallas», PLATODO-27). */
const LOGIN_TEXTS = {
  invalidCredentials: 'Correo o contraseña incorrectos.',
  failed: 'No pudimos iniciar sesión. Inténtalo de nuevo.',
  emailRequired: 'Escribe tu correo.',
  passwordRequired: 'Escribe tu contraseña.',
} as const;

interface LoginFieldErrors {
  readonly email?: string;
  readonly password?: string;
}

/** P1 · Iniciar sesión. No sign-up or password recovery (out of the spec). */
@Component({
  selector: 'app-login-page',
  imports: [Alert, Button, Field],
  templateUrl: './login-page.html',
  styleUrl: './login-page.css',
})
export class LoginPage {
  private readonly session = inject(Session);
  private readonly router = inject(Router);

  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly submitting = signal(false);
  protected readonly fieldErrors = signal<LoginFieldErrors>({});
  protected readonly failure = signal<string | null>(null);
  protected readonly sessionNotice = computed(() => {
    switch (this.session.notice()) {
      case 'SESSION_EXPIRED':
        return COMMON_MESSAGES.sessionExpired;
      case 'USER_LOAD_FAILED':
        return LOGIN_TEXTS.failed;
      default:
        return null;
    }
  });

  private readonly emailField = viewChild.required<Field>('emailField');
  private readonly passwordField = viewChild.required<Field>('passwordField');

  protected async submit(event: Event): Promise<void> {
    event.preventDefault();
    if (this.submitting() || !this.validate()) {
      return;
    }
    this.submitting.set(true);
    this.failure.set(null);
    try {
      await this.session.signIn({ email: this.email().trim(), password: this.password() });
      await this.router.navigateByUrl(HOME_URL);
    } catch (error) {
      const code = asApiError(error).code;
      this.failure.set(
        code === 'INVALID_CREDENTIALS' ? LOGIN_TEXTS.invalidCredentials : LOGIN_TEXTS.failed,
      );
    } finally {
      this.submitting.set(false);
    }
  }

  private validate(): boolean {
    const errors: LoginFieldErrors = {
      email: this.email().trim() ? undefined : LOGIN_TEXTS.emailRequired,
      password: this.password() ? undefined : LOGIN_TEXTS.passwordRequired,
    };
    this.fieldErrors.set(errors);
    if (errors.email) {
      this.emailField().focus();
    } else if (errors.password) {
      this.passwordField().focus();
    }
    return !errors.email && !errors.password;
  }
}
