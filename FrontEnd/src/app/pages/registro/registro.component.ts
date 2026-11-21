import { Component, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { API_CONFIG } from '../../core/api.config';
import { nuevaPersona } from '../../models/usuario.model';

@Component({ selector: 'app-registro', standalone: true, imports: [FormsModule, RouterLink],
  templateUrl: './registro.component.html', styleUrl: './registro.component.css' })
export class RegistroComponent {
  private readonly http = inject(HttpClient);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly completado = signal(false);
  nombreUsuario = ''; email = ''; password = '';
  persona = nuevaPersona();
  erroresFormulario(form: NgForm): string[] {
    const nombres: Record<string, string> = { nombreUsuario: 'Nombre de usuario', email: 'Correo electrónico',
      password: 'Contraseña', nombres: 'Nombres', apellidos: 'Apellidos', tipoDocumento: 'Tipo de documento',
      numeroDocumento: 'Número de documento', telefono: 'Teléfono', direccion: 'Dirección' };
    return Object.entries(form.controls).flatMap(([nombre, control]) => {
      if (!control.invalid || !(control.dirty || control.touched || form.submitted)) return [];
      const campo = nombres[nombre] ?? nombre;
      if (control.hasError('required')) return [campo + ': completa este campo.'];
      if (control.hasError('email')) return [campo + ': escribe una dirección válida.'];
      if (control.hasError('minlength')) return [campo + ': usa al menos ' + control.getError('minlength').requiredLength + ' caracteres.'];
      if (control.hasError('maxlength')) return [campo + ': supera la longitud permitida.'];
      return [campo + ': revisa el valor ingresado.'];
    });
  }

  registrar(form: NgForm): void {
    if (form.invalid || this.ocupado() || this.completado()) { form.control.markAllAsTouched(); return; }
    if (!this.nombreUsuario.trim() || Object.values(this.persona).some(v => !String(v).trim())) {
      this.error.set('Completa todos los campos obligatorios.'); return;
    }
    this.ocupado.set(true); this.error.set('');
    this.http.post(API_CONFIG.baseUrl + '/usuarios/register', {
      nombreUsuario: this.nombreUsuario.trim(), email: this.email.trim(), password: this.password,
      persona: this.persona,
    }).subscribe({ next: () => { this.ocupado.set(false); this.completado.set(true); this.password = ''; },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false);
        this.error.set(e.error?.detail ?? e.error?.message ?? 'No se pudo crear tu cuenta. Revisa tus datos e inténtalo nuevamente.'); } });
  }
}
