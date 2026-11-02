import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  readonly errorMessage = signal('');
  readonly enviando = signal(false);

  loginForm: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid || this.enviando()) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.enviando.set(true);

    this.authService.login(this.loginForm.getRawValue()).subscribe({
      next: (response) => {
        localStorage.setItem('token', response.token);
        localStorage.setItem('usuario', JSON.stringify(response));

        this.enviando.set(false);

        // Aún no hay dashboard; después del login regresamos al catálogo.
        void this.router.navigateByUrl('/catalogo');
      },
      error: (error) => {
        console.error('Error al iniciar sesión.', error);
        this.errorMessage.set(
          'No se pudo iniciar sesión. Verifica tu correo y contraseña.'
        );
        this.enviando.set(false);
      }
    });
  }
}
