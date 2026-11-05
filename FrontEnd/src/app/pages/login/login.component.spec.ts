import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../services/auth';
import { LoginComponent } from './login.component';

describe('Destino después de iniciar sesión', () => {
  it.each([
    ['CLIENTE', '/mis-reservas'],
    ['GUARDIAN', '/guardian/reservas'],
    ['ADMIN', '/catalogo']
  ])('envía el rol %s a %s', (rol, destino) => {
    TestBed.configureTestingModule({
      imports: [LoginComponent], providers: [provideRouter([]), {
        provide: AuthService, useValue: {
          login: () => of({ token: 'token', userId: 5, nombre: 'Antonella', email: 'antonella@example.com', rol }),
          guardarSesion: () => {}
        }
      }]
    });
    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance.loginForm.setValue({ email: 'antonella@example.com', password: 'prueba' });
    fixture.componentInstance.onSubmit();
    expect(navegar).toHaveBeenCalledWith(destino);
    fixture.destroy();
  });
});
