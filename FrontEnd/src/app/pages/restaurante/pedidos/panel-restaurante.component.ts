import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, Subscription, catchError, debounceTime, defer, filter, finalize, map, merge, of, switchMap } from 'rxjs';
import { ESTADOS_PEDIDO, EstadoPedido, PanelPedidos, PedidoPanel, PedidoRestaurante, Restaurante, etiquetaEstado, etiquetaPago } from '../../../models/restaurante.model';
import { AuthService } from '../../../services/auth';
import { UsuariosService } from '../../../services/usuarios.service';
import { RestauranteService } from '../../../services/restaurante.service';
import { PedidosRestauranteService } from '../../../services/pedidos-restaurante.service';
import { NotificacionesService } from '../../../services/notificaciones.service';
import { ActualizacionAutomaticaService } from '../../../services/actualizacion-automatica.service';
import { mensajeErrorApi } from '../../../core/error-api';
import { ConfirmarDirective } from '../../../shared/confirmar/confirmar.directive';
import { FormularioDirective } from '../../../shared/formulario/formulario.directive';
import { VentanaModalComponent } from '../../../shared/ventana-modal/ventana-modal.component';
import { ComandaComponent } from '../../../shared/comanda/comanda.component';
import { DisponibilidadCartaComponent } from './disponibilidad-carta.component';

@Component({ selector: 'app-panel-restaurante', standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ConfirmarDirective, FormularioDirective, VentanaModalComponent, ComandaComponent, DisponibilidadCartaComponent],
  templateUrl: './panel-restaurante.component.html', styleUrls: ['../restaurante.css', './panel-restaurante.component.css'] })
export class PanelRestauranteComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly locales = signal<Restaurante[]>([]);
  readonly local = signal<Restaurante | null>(null);
  readonly iniciando = signal(true);
  readonly errorInicial = signal('');
  readonly panel = signal<PanelPedidos | null>(null);
  readonly cargando = signal(false);
  readonly errorPanel = signal('');
  readonly estado = signal<EstadoPedido | ''>('');
  readonly cabana = signal('');
  readonly pagina = signal(0);
  readonly vista = signal<'pedidos' | 'carta'>('pedidos');
  readonly ultimaActualizacion = signal<Date | null>(null);
  readonly idPedido = signal<number | null>(null);
  readonly detalle = signal<PedidoRestaurante | null>(null);
  readonly cargandoDetalle = signal(false);
  readonly errorDetalle = signal('');
  readonly errorAccion = signal('');
  readonly ocupado = signal(false);
  readonly rechazando = signal(false);
  readonly estados = ESTADOS_PEDIDO;
  readonly pasos: EstadoPedido[] = ['NUEVO', 'EN_PREPARACION', 'EN_ENTREGA', 'FINALIZADO'];
  readonly etiquetaEstado = etiquetaEstado;
  readonly etiquetaPago = etiquetaPago;
  motivo = '';
  private readonly api = inject(PedidosRestauranteService);
  private readonly restaurantes = inject(RestauranteService);
  private readonly usuarios = inject(UsuariosService);
  private readonly avisos = inject(NotificacionesService);
  private readonly destruir = inject(DestroyRef);
  private readonly automatico = inject(ActualizacionAutomaticaService);
  private readonly actualizar = new Subject<void>();
  private readonly buscar = new Subject<void>();
  private peticionDetalle?: Subscription;

  ngOnInit(): void {
    this.buscar.pipe(debounceTime(350), takeUntilDestroyed(this.destruir)).subscribe(() => { this.pagina.set(0); this.recargar(); });
    merge(this.actualizar.pipe(map(() => false)), this.automatico.observar().pipe(filter(() => !this.ocupado() && this.vista() === 'pedidos'), map(() => true)))
      .pipe(filter(() => !!this.local()), switchMap(silencioso => defer(() => {
        if (!silencioso) this.cargando.set(true); this.errorPanel.set('');
        return this.api.panel(this.local()!.idRestaurante, this.estado(), this.cabana(), this.pagina()).pipe(
          catchError(error => { this.errorPanel.set(mensajeErrorApi(error, 'No se pudieron cargar los pedidos.')); return of(null); }),
          finalize(() => this.cargando.set(false)),
        );
      })), takeUntilDestroyed(this.destruir)).subscribe(datos => { if (datos) { this.panel.set(datos); this.ultimaActualizacion.set(new Date()); } });
    this.automatico.observar(true).pipe(takeUntilDestroyed(this.destruir)).subscribe(() => {
      if (this.idPedido() && !this.ocupado() && !this.rechazando()) this.cargarDetalle(true);
      else if (!this.local() && !this.iniciando()) this.iniciar();
    });
    this.iniciar();
  }
  iniciar(): void {
    this.iniciando.set(true); this.errorInicial.set('');
    if (this.auth.usuarioActual()?.rol === 'ADMIN') {
      this.restaurantes.listar().pipe(takeUntilDestroyed(this.destruir), finalize(() => this.iniciando.set(false))).subscribe({
        next: locales => { this.locales.set(locales); this.local.set(locales.find(l => l.idRestaurante === this.local()?.idRestaurante) ?? locales[0] ?? null); this.recargar(); },
        error: error => this.errorInicial.set(mensajeErrorApi(error, 'No se pudieron cargar los restaurantes.')),
      });
    } else {
      this.usuarios.perfil().pipe(switchMap(perfil => this.restaurantes.miLocal(perfil.id)), takeUntilDestroyed(this.destruir), finalize(() => this.iniciando.set(false))).subscribe({
        next: local => { this.local.set(local); this.recargar(); },
        error: error => this.errorInicial.set(mensajeErrorApi(error, 'No se encontró un restaurante asignado a tu cuenta. Solicita al administrador que revise tu asignación.')),
      });
    }
  }
  seleccionarLocal(id: number): void { this.local.set(this.locales().find(l => l.idRestaurante === id) ?? null); this.panel.set(null); this.pagina.set(0); this.recargar(); }
  recargar(): void { this.actualizar.next(); }
  cambiarEstadoFiltro(estado: EstadoPedido | ''): void { this.estado.set(estado); this.pagina.set(0); this.recargar(); }
  buscarCabana(texto: string): void { this.cabana.set(texto); this.buscar.next(); }
  limpiar(): void { this.estado.set(''); this.cabana.set(''); this.pagina.set(0); this.recargar(); }
  cambiarPagina(pagina: number): void { this.pagina.set(pagina); this.recargar(); }
  abrir(pedido: PedidoPanel): void { this.idPedido.set(pedido.idPedido); this.detalle.set(null); this.rechazando.set(false); this.motivo = ''; this.cargarDetalle(); }
  cargarDetalle(silencioso = false): void {
    if (!this.local() || !this.idPedido()) return;
    if (silencioso && this.peticionDetalle && !this.peticionDetalle.closed) return;
    this.peticionDetalle?.unsubscribe(); if (!silencioso) { this.cargandoDetalle.set(true); this.errorAccion.set(''); } this.errorDetalle.set('');
    this.peticionDetalle = this.api.detalle(this.local()!.idRestaurante, this.idPedido()!)
      .pipe(takeUntilDestroyed(this.destruir), finalize(() => this.cargandoDetalle.set(false))).subscribe({
        next: detalle => this.detalle.set(detalle), error: error => this.errorDetalle.set(mensajeErrorApi(error, 'No se pudo consultar el pedido.')),
      });
  }
  cerrarDetalle(): void { if (this.ocupado()) return; this.peticionDetalle?.unsubscribe(); this.idPedido.set(null); this.detalle.set(null); this.recargar(); }
  cambiarEstado(estado: EstadoPedido): void {
    const pedido = this.detalle(); if (!pedido || this.ocupado() || (estado === 'RECHAZADO' && !this.motivo.trim())) return;
    this.ocupado.set(true); this.errorAccion.set('');
    this.api.cambiarEstado(pedido.idRestaurante, pedido.idPedido, estado, estado === 'RECHAZADO' ? this.motivo : undefined)
      .pipe(takeUntilDestroyed(this.destruir), finalize(() => this.ocupado.set(false))).subscribe({
        next: detalle => { this.detalle.set(detalle); this.rechazando.set(false); this.motivo = ''; this.recargar(); this.avisos.mostrar('exito', estado === 'FINALIZADO' ? 'Entrega y cobro confirmados.' : 'Pedido actualizado: ' + etiquetaEstado(estado) + '.'); },
        error: error => { this.errorAccion.set(mensajeErrorApi(error, 'No se pudo actualizar el pedido. El estado se comprobará automáticamente.')); this.cargarDetalle(true); },
      });
  }
  pasoCompletado(paso: EstadoPedido): boolean { return this.pasos.indexOf(paso) <= this.pasos.indexOf(this.detalle()?.estadoPedido ?? 'NUEVO'); }
}
