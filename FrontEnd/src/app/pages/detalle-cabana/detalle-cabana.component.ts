import { CommonModule, isPlatformBrowser } from '@angular/common';
import {
  Component,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
  computed,
  inject,
  signal
} from '@angular/core';
import {
  ActivatedRoute,
  Router,
  RouterLink
} from '@angular/router';
import { Subscription } from 'rxjs';
import { CabanaDetalle } from '../../models/cabana.model';
import {
  CotizacionReservaResponse,
  ReservaResponse
} from '../../models/reserva.model';
import { AuthService } from '../../services/auth';
import { CabanaService } from '../../services/cabana.service';
import { ReservasService } from '../../services/reservas.service';
import { CheckoutRetornoService } from '../../services/checkout-retorno.service';
import { PagoModalComponent } from '../../shared/pago-modal/pago-modal.component';

import { ResenasCabanaComponent } from './resenas-cabana.component';

interface DiaCalendario {
  fecha: string;
  numero: number;
  mesActual: boolean;
  pasado: boolean;
}

@Component({
  selector: 'app-detalle-cabana',
  standalone: true,
  imports: [CommonModule, RouterLink, PagoModalComponent, ResenasCabanaComponent],
  templateUrl: './detalle-cabana.component.html',
  styleUrl: './detalle-cabana.component.css'
})
export class DetalleCabanaComponent implements OnInit, OnDestroy {
  readonly cabana = signal<CabanaDetalle | null>(null);
  readonly cargando = signal(true);
  readonly mensajeError = signal('');
  readonly indiceImagen = signal(0);

  readonly modalReservaAbierto = signal(false);
  readonly confirmacionReservaAbierta = signal(false);
  readonly cotizacion = signal<CotizacionReservaResponse | null>(null);
  readonly cargandoCotizacion = signal(false);
  readonly mensajeCotizacion = signal('');
  readonly creandoReserva = signal(false);
  readonly mensajeReserva = signal('');
  readonly reservaCreada = signal<ReservaResponse | null>(null);

  readonly fechasOcupadas = signal<ReadonlySet<string>>(new Set());
  readonly cargandoFechasOcupadas = signal(false);
  readonly mensajeFechasOcupadas = signal('');

  readonly fechaIngreso = signal('');
  readonly fechaSalida = signal('');
  readonly adultos = signal(1);
  readonly ninos = signal(0);

  readonly mesMostrado = signal(
    new Date(new Date().getFullYear(), new Date().getMonth(), 1)
  );

  readonly fechaMinima = this.fechaLocal(new Date());

  readonly totalHuespedes = computed(
    () => this.adultos() + this.ninos()
  );

  readonly tituloMes = computed(() =>
    new Intl.DateTimeFormat('es-PE', {
      month: 'long',
      year: 'numeric'
    }).format(this.mesMostrado())
  );

  readonly diasCalendario = computed(
    () => this.generarDias()
  );

  readonly imagenes = computed(() => {
    const detalle = this.cabana();

    if (!detalle) return [];

    return [
      detalle.imagenPrincipal,
      ...(detalle.imagenesSecundarias ?? [])
    ].filter(Boolean);
  });

  readonly imagenActual = computed(
    () => this.imagenes()[this.indiceImagen()] ?? ''
  );

  readonly puedeCotizar = computed(() => {
    const detalle = this.cabana();

    return Boolean(
      detalle &&
      this.fechaIngreso() >= this.fechaMinima &&
      this.fechaSalida() > this.fechaIngreso() &&
      this.totalHuespedes() >= 1 &&
      this.totalHuespedes() <= detalle.aforoMaximo
    );
  });

  readonly mensajeSeleccionFechas = computed(() => {
    if (this.reservaCreada()) {
      return this.reservaCreada()?.estadoReserva === 'CONFIRMADA'
        ? 'Tu reserva está confirmada para estas fechas.'
        : 'Hay un bloqueo temporal activo para estas fechas.';
    }

    if (!this.fechaIngreso()) {
      return 'Selecciona la fecha de entrada. Los días rojos tienen noches ocupadas.';
    }

    if (!this.fechaSalida()) {
      return 'Ahora selecciona la fecha de salida posterior a la entrada.';
    }

    if (this.fechaSalida() <= this.fechaIngreso()) {
      return 'La fecha de salida debe ser posterior a la entrada.';
    }

    if (this.cargandoCotizacion()) {
      return 'Consultando disponibilidad para el rango seleccionado...';
    }

    return 'Las fechas se confirman al consultar la disponibilidad.';
  });

  readonly puedeCrearReserva = computed(() =>
    Boolean(
      this.cotizacion()?.disponible &&
      !this.cargandoCotizacion() &&
      !this.creandoReserva() &&
      !this.reservaCreada()
    )
  );

  readonly transaccionRetorno = signal(0);
  private readonly retornoCheckout = inject(CheckoutRetornoService);
  pagoConfirmado(): void {
    if (this.temporizadorReserva) clearTimeout(this.temporizadorReserva);
    this.reservaCreada.update(r => r ? { ...r, estadoReserva: 'CONFIRMADA' } : r);
  }
  private readonly platformId = inject(PLATFORM_ID);
  private cotizacionSubscription?: Subscription;
  private fechasOcupadasSubscription?: Subscription;
  private temporizadorReserva?: ReturnType<typeof setTimeout>;
  private idCabana = 0;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly cabanaService: CabanaService,
    private readonly reservasService: ReservasService
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    this.idCabana = Number(this.route.snapshot.paramMap.get('id'));

    if (!Number.isInteger(this.idCabana) || this.idCabana <= 0) {
      this.mensajeError.set('El identificador de la cabaña no es válido.');
      this.cargando.set(false);
      return;
    }

    const params = this.route.snapshot.queryParamMap;
    const fechaEntradaParametro = params.get('fechaIngreso') ?? '';
    const fechaSalidaParametro = params.get('fechaSalida') ?? '';
    const entradaValida = fechaEntradaParametro >= this.fechaMinima;

    this.fechaIngreso.set(entradaValida ? fechaEntradaParametro : '');
    this.fechaSalida.set(
      entradaValida && fechaSalidaParametro > fechaEntradaParametro
        ? fechaSalidaParametro
        : ''
    );
    this.adultos.set(Math.max(1, Number(params.get('adultos') ?? 1)));
    this.ninos.set(Math.max(0, Number(params.get('ninos') ?? 0)));

    const abrirReserva = params.get('abrirReserva') === 'true';

    if (abrirReserva && !this.authService.estaAutenticado()) {
      this.irAlLoginConRetorno();
      return;
    }

    const idTransaccion = Number(params.get('transaccion'));
    const contexto = this.retornoCheckout.obtener(idTransaccion);
    if (contexto && contexto.reserva.idCabana === this.idCabana) {
      this.reservaCreada.set(contexto.reserva);
      this.cotizacion.set(contexto.cotizacion ?? null);
      this.fechaIngreso.set(contexto.reserva.fechaIngreso); this.fechaSalida.set(contexto.reserva.fechaSalida);
      this.adultos.set(contexto.adultos); this.ninos.set(contexto.ninos); this.indiceImagen.set(contexto.imagen);
      this.transaccionRetorno.set(idTransaccion); this.modalReservaAbierto.set(true);
      window.scrollTo(0, contexto.scroll);
    } else this.modalReservaAbierto.set(abrirReserva);
    this.cargarDetalle();
  }

  ngOnDestroy(): void {
    this.cotizacionSubscription?.unsubscribe();
    this.fechasOcupadasSubscription?.unsubscribe();

    if (this.temporizadorReserva) {
      clearTimeout(this.temporizadorReserva);
    }
  }

  seleccionarImagen(indice: number): void {
    const total = this.imagenes().length;

    if (!Number.isInteger(indice) || indice < 0 || indice >= total) {
      return;
    }

    this.indiceImagen.set(indice);
  }

  imagenAnterior(): void {
    const total = this.imagenes().length;
    if (total <= 1) return;

    this.indiceImagen.update(indice => (indice - 1 + total) % total);
  }

  imagenSiguiente(): void {
    const total = this.imagenes().length;
    if (total <= 1) return;

    this.indiceImagen.update(indice => (indice + 1) % total);
  }

  private cargarDetalle(): void {
    this.cargando.set(true);

    this.cabanaService.obtenerDetalle(this.idCabana).subscribe({
      next: detalle => {
        this.cabana.set(detalle);
        this.cargando.set(false);

        if (this.modalReservaAbierto()) {
          this.consultarFechasOcupadas();

          if (this.puedeCotizar() && !this.reservaCreada()) {
            this.consultarDisponibilidad();
          }
        }
      },
      error: error => {
        console.error('No se pudo cargar el detalle de la cabaña.', error);
        this.mensajeError.set(
          'No se encontró la cabaña o no se pudo cargar su información.'
        );
        this.cargando.set(false);
      }
    });
  }

  abrirModalReserva(): void {
    if (!this.authService.estaAutenticado()) {
      this.irAlLoginConRetorno();
      return;
    }

    this.modalReservaAbierto.set(true);
    this.mensajeReserva.set('');
    this.consultarFechasOcupadas();

    if (this.puedeCotizar()) {
      this.consultarDisponibilidad();
    }
  }

  cerrarModalReserva(): void {
    this.modalReservaAbierto.set(false);
    this.confirmacionReservaAbierta.set(false);
  }

  cerrarConfirmacionReserva(): void {
    // Cierra solo la confirmación. El calendario queda detrás, abierto.
    this.confirmacionReservaAbierta.set(false);
  }

  seleccionarDia(dia: DiaCalendario): void {
    if (
      dia.pasado ||
      dia.fecha < this.fechaMinima ||
      !dia.mesActual ||
      this.reservaCreada()
    ) {
      return;
    }

    const entradaActual = this.fechaIngreso();
    const salidaActual = this.fechaSalida();
    const vaAIniciarRango =
      !entradaActual || Boolean(salidaActual) || dia.fecha <= entradaActual;

    // Una noche ocupada no puede ser entrada. Sí se permite probarla como salida,
    // pues ese día puede ser el check-out de otra reserva.
    if (vaAIniciarRango && this.fechaOcupada(dia.fecha)) {
      this.mensajeReserva.set(
        'Esa fecha tiene una noche ocupada. Selecciona otra fecha de entrada.'
      );
      return;
    }

    this.mensajeReserva.set('');
    this.cotizacion.set(null);
    this.mensajeCotizacion.set('');

    if (vaAIniciarRango) {
      this.fechaIngreso.set(dia.fecha);
      this.fechaSalida.set('');
      return;
    }

    this.fechaSalida.set(dia.fecha);
    this.consultarDisponibilidad();
  }

  cambiarAdultos(cambio: number): void {
    const detalle = this.cabana();
    if (!detalle || this.reservaCreada()) return;

    const nuevo = this.adultos() + cambio;

    if (nuevo < 1 || nuevo + this.ninos() > detalle.aforoMaximo) return;

    this.adultos.set(nuevo);
    this.actualizarCotizacion();
  }

  cambiarNinos(cambio: number): void {
    const detalle = this.cabana();
    if (!detalle || this.reservaCreada()) return;

    const nuevo = this.ninos() + cambio;

    if (nuevo < 0 || this.adultos() + nuevo > detalle.aforoMaximo) return;

    this.ninos.set(nuevo);
    this.actualizarCotizacion();
  }

  mesAnterior(): void {
    const ahora = new Date();
    const mesActual = new Date(ahora.getFullYear(), ahora.getMonth(), 1);
    const mostrado = this.mesMostrado();

    if (mostrado <= mesActual) return;

    this.mesMostrado.set(
      new Date(mostrado.getFullYear(), mostrado.getMonth() - 1, 1)
    );
    this.consultarFechasOcupadas();
  }

  mesSiguiente(): void {
    const mostrado = this.mesMostrado();

    this.mesMostrado.set(
      new Date(mostrado.getFullYear(), mostrado.getMonth() + 1, 1)
    );
    this.consultarFechasOcupadas();
  }

  fechaOcupada(fecha: string): boolean {
    return this.fechasOcupadas().has(fecha);
  }

  estaEnEstadia(fecha: string): boolean {
    return Boolean(
      this.fechaIngreso() &&
      this.fechaSalida() &&
      fecha >= this.fechaIngreso() &&
      fecha <= this.fechaSalida()
    );
  }

  esDiaLimite(fecha: string): boolean {
    return fecha === this.fechaIngreso() || fecha === this.fechaSalida();
  }

  rangoNoDisponible(fecha: string): boolean {
    return Boolean(
      this.cotizacion() &&
      !this.cotizacion()?.disponible &&
      this.estaEnEstadia(fecha)
    );
  }

  consultarFechasOcupadas(): void {
    if (!this.idCabana || !this.modalReservaAbierto()) return;

    const mes = this.mesMostrado();
    const desde = new Date(mes.getFullYear(), mes.getMonth(), 1);
    const ultimoDia = new Date(mes.getFullYear(), mes.getMonth() + 1, 0);

    const desdeTexto = this.fechaLocal(desde);
    const hastaTexto = this.fechaLocal(ultimoDia);

    this.fechasOcupadasSubscription?.unsubscribe();
    this.cargandoFechasOcupadas.set(true);
    this.mensajeFechasOcupadas.set('');

    this.fechasOcupadasSubscription = this.reservasService
      .consultarFechasOcupadas(
        this.idCabana,
        desdeTexto,
        hastaTexto
      )
      .subscribe({
        next: respuesta => {
          this.fechasOcupadas.set(
            new Set(respuesta.fechasOcupadas ?? [])
          );
          this.cargandoFechasOcupadas.set(false);
        },
        error: error => {
          console.error('No se pudieron cargar las fechas ocupadas.', error);
          this.mensajeFechasOcupadas.set(
            'No se pudo cargar la leyenda de disponibilidad de este mes.'
          );
          this.cargandoFechasOcupadas.set(false);
        }
      });
  }

  consultarDisponibilidad(): void {
    if (!this.cabana() || !this.puedeCotizar()) {
      this.cotizacion.set(null);
      this.cargandoCotizacion.set(false);
      return;
    }

    this.cotizacionSubscription?.unsubscribe();
    this.cargandoCotizacion.set(true);
    this.mensajeCotizacion.set('');
    this.cotizacion.set(null);

    this.cotizacionSubscription = this.reservasService
      .consultarDisponibilidad(
        this.idCabana,
        this.fechaIngreso(),
        this.fechaSalida(),
        this.totalHuespedes()
      )
      .subscribe({
        next: resultado => {
          this.cotizacion.set(resultado);
          this.mensajeCotizacion.set(resultado.mensaje);
          this.cargandoCotizacion.set(false);
        },
        error: error => {
          console.error('Error consultando disponibilidad.', error);
          this.mensajeCotizacion.set(
            this.obtenerMensajeError(
              error,
              'No se pudo consultar la disponibilidad. Intenta nuevamente.'
            )
          );
          this.cargandoCotizacion.set(false);
        }
      });
  }

  confirmarBloqueo(): void {
    if (!this.authService.estaAutenticado()) {
      this.irAlLoginConRetorno();
      return;
    }

    if (!this.puedeCrearReserva()) return;

    this.creandoReserva.set(true);
    this.mensajeReserva.set('');

    this.reservasService
      .crearReservaTemporal({
        idCabana: this.idCabana,
        fechaIngreso: this.fechaIngreso(),
        fechaSalida: this.fechaSalida(),
        cantidadHuespedes: this.totalHuespedes()
      })
      .subscribe({
        next: reserva => {
          this.reservaCreada.set(reserva);
          this.creandoReserva.set(false);
          this.confirmacionReservaAbierta.set(true);

          // Actualiza el calendario para marcar el nuevo bloqueo.
          this.consultarFechasOcupadas();
          this.programarExpiracionReserva(reserva);
        },
        error: error => {
          console.error('No se pudo crear el bloqueo temporal.', error);
          this.mensajeReserva.set(
            this.obtenerMensajeError(
              error,
              'No se pudo bloquear temporalmente la cabaña. Vuelve a consultar las fechas.'
            )
          );
          this.creandoReserva.set(false);
          this.consultarDisponibilidad();
          this.consultarFechasOcupadas();
        }
      });
  }

  formatearTarifa(valor: number): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(valor);
  }

  formatearFecha(fecha: string): string {
    if (!fecha) return '—';

    const [anio, mes, dia] = fecha.split('-').map(Number);

    return new Intl.DateTimeFormat('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(new Date(anio, mes - 1, dia));
  }

  formatearFechaHora(fechaHora: string): string {
    if (!fechaHora) return '—';

    // LocalDateTime del backend no lleva zona; se interpreta como hora local.
    return new Intl.DateTimeFormat('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(fechaHora));
  }

  etiquetaDia(dia: DiaCalendario): string {
    const fecha = this.formatearFecha(dia.fecha);

    if (dia.pasado) return `${fecha}, fecha pasada no disponible`;
    if (!dia.mesActual) return `${fecha}, fuera del mes mostrado`;
    if (this.rangoNoDisponible(dia.fecha)) {
      return `${fecha}, rango seleccionado no disponible`;
    }
    if (this.fechaOcupada(dia.fecha)) {
      return `${fecha}, noche ocupada`;
    }
    if (this.esDiaLimite(dia.fecha)) return `${fecha}, seleccionada`;
    if (this.estaEnEstadia(dia.fecha)) return `${fecha}, dentro de tu estadía`;

    return `${fecha}, disponible`;
  }

  mostrarImagenAlternativa(event: Event): void {
    const imagen = event.target as HTMLImageElement;

    if (imagen.dataset['alternativaUsada'] !== 'true') {
      imagen.dataset['alternativaUsada'] = 'true';
      imagen.src = 'https://placehold.co/900x600?text=Imagen+no+disponible';
    }
  }

  private actualizarCotizacion(): void {
    if (this.puedeCotizar()) {
      this.consultarDisponibilidad();
    } else {
      this.cotizacion.set(null);
      this.mensajeCotizacion.set('');
    }
  }

  private programarExpiracionReserva(reserva: ReservaResponse): void {
    if (this.temporizadorReserva) {
      clearTimeout(this.temporizadorReserva);
    }

    const revisarExpiracion = (): void => {
      if (this.reservaCreada()?.idReserva !== reserva.idReserva) return;

      const venceEn = new Date(reserva.expiraEn).getTime();
      const faltante = venceEn - Date.now();

      if (faltante <= 0) {
        this.reservaCreada.set(null);
        this.confirmacionReservaAbierta.set(false);
        this.mensajeReserva.set(
          'El bloqueo temporal venció. Las fechas se liberaron; consulta su disponibilidad nuevamente.'
        );
        this.consultarFechasOcupadas();

        if (this.puedeCotizar()) {
          this.consultarDisponibilidad();
        }
        return;
      }

      this.temporizadorReserva = setTimeout(
        revisarExpiracion,
        Math.min(faltante, 60_000)
      );
    };

    revisarExpiracion();
  }

  private irAlLoginConRetorno(): void {
    const returnUrl = this.router.createUrlTree(
      ['/catalogo', this.idCabana],
      {
        queryParams: {
          abrirReserva: true,
          fechaIngreso: this.fechaIngreso() || null,
          fechaSalida: this.fechaSalida() || null,
          adultos: this.adultos(),
          ninos: this.ninos()
        }
      }
    ).toString();

    void this.router.navigate(['/login'], {
      queryParams: { returnUrl }
    });
  }

  private generarDias(): DiaCalendario[] {
    const mes = this.mesMostrado();
    const primerDia = new Date(mes.getFullYear(), mes.getMonth(), 1);
    const inicio = new Date(
      primerDia.getFullYear(),
      primerDia.getMonth(),
      1 - primerDia.getDay()
    );

    return Array.from({ length: 42 }, (_, indice) => {
      const fecha = new Date(
        inicio.getFullYear(),
        inicio.getMonth(),
        inicio.getDate() + indice
      );
      const iso = this.fechaLocal(fecha);

      return {
        fecha: iso,
        numero: fecha.getDate(),
        mesActual:
          fecha.getMonth() === mes.getMonth() &&
          fecha.getFullYear() === mes.getFullYear(),
        pasado: iso < this.fechaMinima
      };
    });
  }

  private fechaLocal(fecha: Date): string {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');

    return `${anio}-${mes}-${dia}`;
  }

  private obtenerMensajeError(
    error: any,
    mensajePredeterminado: string
  ): string {
    if (error?.status === 0) {
      return 'No pudimos conectarnos con el servicio de reservas. Intenta nuevamente.';
    }

    if (error?.status === 401) {
      this.authService.cerrarSesion();
      return 'Tu sesión venció. Inicia sesión nuevamente para continuar.';
    }

    if (error?.status === 409) {
      return 'Las fechas dejaron de estar disponibles. Selecciona otro rango.';
    }

    return error?.error?.detail ??
      error?.error?.message ??
      mensajePredeterminado;
  }
}
