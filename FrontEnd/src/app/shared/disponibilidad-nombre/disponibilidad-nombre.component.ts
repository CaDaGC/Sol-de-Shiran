import { Component, effect, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, map, Observable, of, Subject, switchMap, timer } from 'rxjs';
import { DisponibilidadNombre, NombreUsuarioService } from '../../services/nombre-usuario.service';

type EstadoNombre = 'inicial' | 'consultando' | 'disponible' | 'ocupado' | 'error';
interface ResultadoConsulta {
  estado: EstadoNombre;
  resultado: DisponibilidadNombre | null;
}

@Component({
  selector: 'app-disponibilidad-nombre',
  standalone: true,
  templateUrl: './disponibilidad-nombre.component.html',
  styleUrl: './disponibilidad-nombre.component.css',
})
export class DisponibilidadNombreComponent {
  readonly nombre = input('');
  readonly nombreActual = input('');
  readonly revision = input(0);
  readonly seleccionar = output<string>();
  readonly bloqueado = output<boolean>();
  readonly estado = signal<EstadoNombre>('inicial');
  readonly resultado = signal<DisponibilidadNombre | null>(null);
  private readonly servicio = inject(NombreUsuarioService);
  private readonly consultas = new Subject<{ nombre: string; actual: string }>();

  constructor() {
    this.consultas.pipe(
      switchMap(({ nombre, actual }): Observable<ResultadoConsulta> => {
        if (!nombre) return of({ estado: 'inicial', resultado: null });
        if (nombre.toLocaleLowerCase() === actual.toLocaleLowerCase())
          return of({ estado: 'disponible', resultado: null });
        return timer(350).pipe(
          switchMap(() => this.servicio.consultar(nombre)),
          map(resultado => ({ estado: resultado.disponible ? 'disponible' : 'ocupado', resultado }) as ResultadoConsulta),
          catchError(() => of<ResultadoConsulta>({ estado: 'error', resultado: null })),
        );
      }),
      takeUntilDestroyed(),
    ).subscribe(({ estado, resultado }) => {
      this.estado.set(estado);
      this.resultado.set(resultado);
      this.bloqueado.emit(estado === 'ocupado');
    });

    effect(() => {
      const nombre = this.nombre().trim();
      const actual = this.nombreActual().trim();
      this.revision();
      this.resultado.set(null);
      this.estado.set(nombre ? 'consultando' : 'inicial');
      this.bloqueado.emit(!!nombre);
      this.consultas.next({ nombre, actual });
    });
  }
}
