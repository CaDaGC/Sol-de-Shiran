import { Component, DestroyRef, OnChanges, OnInit, SimpleChanges, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, forkJoin } from 'rxjs';
import { AsignacionRestaurante } from '../../../models/restaurante.model';
import { Cuenta } from '../../../models/usuario.model';
import { RestauranteService } from '../../../services/restaurante.service';
import { UsuariosService } from '../../../services/usuarios.service';
import { normalizarRol } from '../../../services/auth';
import { NotificacionesService } from '../../../services/notificaciones.service';
import { mensajeErrorApi } from '../../../core/error-api';
import { ConfirmarDirective } from '../../../shared/confirmar/confirmar.directive';
import { FormularioDirective } from '../../../shared/formulario/formulario.directive';
import { IconoComponent } from '../../../shared/icono/icono.component';

@Component({ selector: 'app-encargados-restaurante', standalone: true, imports: [FormsModule, ConfirmarDirective, FormularioDirective, IconoComponent],
  templateUrl: './encargados-restaurante.component.html', styleUrl: '../restaurante.css' })
export class EncargadosRestauranteComponent implements OnInit, OnChanges {
  readonly idRestaurante = input.required<number>();
  readonly revisionCarga = input(0);
  readonly asignaciones = signal<AsignacionRestaurante[]>([]);
  readonly cuentas = signal<Cuenta[]>([]);
  readonly opciones = computed(() => this.cuentas().filter(u => u.activo && !this.asignaciones().some(a => a.idUsuario === u.id)));
  readonly cargando = signal(false);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly errorCarga = signal('');
  private readonly api = inject(RestauranteService);
  private consultando = false;
  private readonly usuarios = inject(UsuariosService);
  private readonly avisos = inject(NotificacionesService);
  private readonly destruir = inject(DestroyRef);
  seleccionado: number | null = null;
  ngOnInit(): void { this.cargar(); }
  ngOnChanges(cambios: SimpleChanges): void { if (cambios['revisionCarga'] && !cambios['revisionCarga'].firstChange) this.cargar(true); }
  cargar(silencioso = false): void {
    if (this.consultando || (silencioso && (this.ocupado()))) return;
    this.consultando = true;
    if (!silencioso) this.cargando.set(true); this.errorCarga.set('');
    forkJoin({ asignaciones: this.api.asignaciones(this.idRestaurante()), cuentas: this.usuarios.listar() })
      .pipe(takeUntilDestroyed(this.destruir), finalize(() => { this.cargando.set(false); this.consultando = false; })).subscribe({
        next: datos => { this.asignaciones.set(datos.asignaciones); this.cuentas.set(datos.cuentas.filter(u => normalizarRol(u.rol) === 'RESTAURANTE')); },
        error: error => this.errorCarga.set(mensajeErrorApi(error, 'No se pudieron cargar los encargados.')),
      });
  }
  cuenta(id: number): Cuenta | undefined { return this.cuentas().find(u => u.id === id); }
  asignar(): void {
    if (!this.seleccionado || this.ocupado()) return;
    this.ocupado.set(true); this.error.set('');
    this.api.asignar(this.idRestaurante(), this.seleccionado).pipe(takeUntilDestroyed(this.destruir), finalize(() => this.ocupado.set(false))).subscribe({
      next: () => { this.seleccionado = null; this.cargar(); this.avisos.mostrar('exito', 'Encargado asignado al restaurante.'); },
      error: error => this.error.set(mensajeErrorApi(error, 'No se pudo asignar al encargado.')),
    });
  }
  retirar(asignacion: AsignacionRestaurante): void {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.api.retirarAsignacion(this.idRestaurante(), asignacion.idUsuario).pipe(takeUntilDestroyed(this.destruir), finalize(() => this.ocupado.set(false))).subscribe({
      next: () => { this.cargar(); this.avisos.mostrar('exito', 'Asignación retirada. La cuenta se conserva.'); },
      error: error => this.avisos.mostrar('error', mensajeErrorApi(error, 'No se pudo retirar la asignación.')),
    });
  }
}
