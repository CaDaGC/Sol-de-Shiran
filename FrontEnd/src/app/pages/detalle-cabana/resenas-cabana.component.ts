import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { API_CONFIG } from '../../core/api.config';
import { AuthService } from '../../services/auth';

interface Opinion {
  id: number;
  autor: string;
  puntuacion: number;
  comentario: string;
}

interface Pagina {
  opiniones: Opinion[];
  hayMas: boolean;
}

@Component({
  selector: 'app-resenas-cabana',
  imports: [FormsModule],
  templateUrl: './resenas-cabana.component.html',
  styleUrl: './resenas-cabana.component.css',
})
export class ResenasCabanaComponent {
  readonly idCabana = input.required<number>();
  readonly estrellas = [1, 2, 3, 4, 5];
  readonly cliente = () => this.auth.usuarioActual()?.rol === 'CLIENTE';
  readonly opiniones = signal<Opinion[]>([]);
  readonly estancia = signal<number | null>(null);
  readonly consultandoEstancia = signal(false);
  readonly errorEstancia = signal('');
  readonly cargando = signal(false);
  readonly errorLista = signal('');
  readonly hayMas = signal(false);
  readonly enviando = signal(false);
  readonly resultado = signal('');
  readonly errorEnvio = signal('');
  puntuacion = 0;
  comentario = '';

  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private pagina = 0;
  private readonly recargar = signal(0);

  constructor() {
    effect((limpiar) => {
      const id = this.idCabana();
      this.recargar();
      this.opiniones.set([]);
      this.hayMas.set(false);
      this.pagina = 0;
      const solicitud = this.cargarPagina(id, 0);
      limpiar(() => solicitud.unsubscribe());
    });

    effect((limpiar) => {
      const id = this.idCabana();
      const esCliente = this.cliente();
      this.recargar();
      this.estancia.set(null);
      this.errorEstancia.set('');
      this.consultandoEstancia.set(esCliente);
      if (!esCliente) return;
      const solicitud = this.http.get<{ idReserva: number | null }>(this.url(id) + '/elegibilidad')
        .subscribe({
          next: (datos) => {
            this.estancia.set(datos.idReserva);
            this.consultandoEstancia.set(false);
          },
          error: () => {
            this.consultandoEstancia.set(false);
            this.errorEstancia.set('No se pudo verificar tu estancia.');
          },
        });
      limpiar(() => solicitud.unsubscribe());
    });
  }

  cargarMas(): void {
    if (!this.cargando()) this.cargarPagina(this.idCabana(), this.pagina + 1);
  }

  reintentar(): void {
    this.recargar.update(n => n + 1);
  }

  enviar(): void {
    const idReserva = this.estancia();
    const id = this.idCabana();
    const comentario = this.comentario.trim();
    if (!idReserva || !this.estrellas.includes(this.puntuacion) || !comentario || this.enviando()) return;
    this.enviando.set(true);
    this.errorEnvio.set('');
    this.resultado.set('');
    this.http.post<void>(this.url(id), { idReserva, puntuacion: this.puntuacion, comentario })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.enviando.set(false);
          if (id !== this.idCabana()) return;
          this.puntuacion = 0;
          this.comentario = '';
          this.resultado.set('Tu reseña se publicó.');
          this.reintentar();
        },
        error: (error: HttpErrorResponse) => {
          this.enviando.set(false);
          if (id !== this.idCabana()) return;
          this.errorEnvio.set(error.error?.detail ?? 'No se pudo publicar la reseña. Intenta nuevamente.');
          if (error.status === 409) this.reintentar();
        },
      });
  }

  private cargarPagina(id: number, pagina: number) {
    this.cargando.set(true);
    this.errorLista.set('');
    return this.http.get<Pagina>(this.url(id), { params: { pagina } })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (datos) => {
          if (id !== this.idCabana()) return;
          this.opiniones.update(actual => pagina === 0 ? datos.opiniones : [...actual, ...datos.opiniones]);
          this.hayMas.set(datos.hayMas);
          this.pagina = pagina;
          this.cargando.set(false);
        },
        error: () => {
          if (id !== this.idCabana()) return;
          this.cargando.set(false);
          this.errorLista.set('No se pudieron cargar las reseñas.');
        },
      });
  }

  private url(id: number): string {
    return API_CONFIG.baseUrl + '/resenas/cabanas/' + id;
  }
}
