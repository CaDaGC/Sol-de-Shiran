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

interface DiaCalendario {
  fecha: string;
  numero: number;
  mesActual: boolean;
  pasado: boolean;
}

@Component({
  selector: 'app-detalle-cabana',
  standalone: true,
  imports: [CommonModule, RouterLink],
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

  readonly diasCalendario = computed(() => this.generarDias());

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
      this.fechaIngreso() &&
      this.fechaIngreso() >= this.fechaMinima &&
      this.fechaSalida() > this.fechaIngreso() &&
      this.totalHuespedes() >= 1 &&
      this.totalHuespedes() <= detalle.aforoMaximo
    );
  });

  readonly mensajeSeleccionFechas = computed(() => {
    if (this.reservaCreada()) {
      return 'Las fechas quedaron bloqueadas temporalmente y ya no pueden modificarse.';
    }

    if (!this.fechaIngreso()) {
      return 'Selecciona primero la fecha de entrada. Los días pasados no están disponibles.';
    }

    if (this.fechaIngreso() < this.fechaMinima) {
      return 'La fecha de entrada no puede estar en el pasado.';
    }

    if (!this.fechaSalida()) {
      return 'Ahora selecciona una fecha de salida posterior a la entrada.';
    }

    if (this.fechaSalida() <= this.fechaIngreso()) {
      return 'La fecha de salida debe ser posterior a la fecha de entrada.';
    }

    return 'Fechas válidas. Estamos comprobando la disponibilidad y los montos.';
  });

  readonly puedeCrearReserva = computed(() =>
    Boolean(
      this.cotizacion()?.disponible &&
      !this.cargandoCotizacion() &&
      !this.creandoReserva() &&
      !this.reservaCreada()
    )
  );

  private readonly platformId = inject(PLATFORM_ID);
  private cotizacionSubscription?: Subscription;
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
    const fechaIngresoParametro = params.get('fechaIngreso') ?? '';
    const fechaSalidaParametro = params.get('fechaSalida') ?? '';
    const fechaIngresoValida = fechaIngresoParametro >= this.fechaMinima;

    this.fechaIngreso.set(fechaIngresoValida ? fechaIngresoParametro : '');
    this.fechaSalida.set(
      fechaIngresoValida && fechaSalidaParametro > fechaIngresoParametro
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

    this.modalReservaAbierto.set(abrirReserva);
    this.cargarDetalle();
  }

  ngOnDestroy(): void {
    this.cotizacionSubscription?.unsubscribe();
  }

  seleccionarImagen(indice: number): void {
    const totalImagenes = this.imagenes().length;

    if (!Number.isInteger(indice) || indice < 0 || indice >= totalImagenes) {
      return;
    }

    this.indiceImagen.set(indice);
  }

  imagenAnterior(): void {
    const totalImagenes = this.imagenes().length;

    if (totalImagenes === 0) return;

    this.indiceImagen.update(
      indice => (indice - 1 + totalImagenes) % totalImagenes
    );
  }

  imagenSiguiente(): void {
    const totalImagenes = this.imagenes().length;

    if (totalImagenes === 0) return;

    this.indiceImagen.update(
      indice => (indice + 1) % totalImagenes
    );
  }

  private cargarDetalle(): void {
    this.cargando.set(true);

    this.cabanaService.obtenerDetalle(this.idCabana).subscribe({
      next: (detalle) => {
        this.cabana.set(detalle);
        this.cargando.set(false);

        if (this.modalReservaAbierto() && this.puedeCotizar()) {
          this.consultarDisponibilidad();
        }
      },
      error: (error) => {
        console.error('No se pudo cargar el detalle de la cabaña.', error);
        this.mensajeError.set(
          'No se encontró la cabaña o no se pudo cargar su información.'
        );
        this.cargando.set(false);
      }
    });
  }

  abrirModalReserva(): void {
    if (this.reservaCreada()) return;

    if (!this.authService.estaAutenticado()) {
      this.irAlLoginConRetorno();
      return;
    }

    this.modalReservaAbierto.set(true);
    this.mensajeReserva.set('');

    if (this.puedeCotizar()) {
      this.consultarDisponibilidad();
    }
  }

  cerrarModalReserva(): void {
    this.modalReservaAbierto.set(false);
    this.confirmacionReservaAbierta.set(false);
  }

  cerrarConfirmacionReserva(): void {
    this.confirmacionReservaAbierta.set(false);
    this.modalReservaAbierto.set(false);
  }

  seleccionarDia(dia: DiaCalendario): void {
    if (
      dia.pasado ||
      dia.fecha < this.fechaMinima ||
      !dia.mesActual ||
      this.reservaCreada()
    ) return;

    this.cotizacion.set(null);
    this.mensajeCotizacion.set('');
    this.mensajeReserva.set('');

    const ingresoActual = this.fechaIngreso();
    const salidaActual = this.fechaSalida();

    if (!ingresoActual || salidaActual || dia.fecha <= ingresoActual) {
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
    const actual = new Date(ahora.getFullYear(), ahora.getMonth(), 1);
    const mostrado = this.mesMostrado();

    if (mostrado <= actual) return;

    this.mesMostrado.set(
      new Date(mostrado.getFullYear(), mostrado.getMonth() - 1, 1)
    );
  }

  mesSiguiente(): void {
    const mostrado = this.mesMostrado();

    this.mesMostrado.set(
      new Date(mostrado.getFullYear(), mostrado.getMonth() + 1, 1)
    );
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
        next: (resultado) => {
          this.cotizacion.set(resultado);
          this.mensajeCotizacion.set(resultado.mensaje);
          this.cargandoCotizacion.set(false);
        },
        error: (error) => {
          console.error('Error consultando disponibilidad.', error);
          this.mensajeCotizacion.set(this.obtenerMensajeError(
            error,
            'No se pudo consultar la disponibilidad. Intenta nuevamente.'
          ));
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
        next: (reserva) => {
          this.reservaCreada.set(reserva);
          this.creandoReserva.set(false);
          this.confirmacionReservaAbierta.set(true);
        },
        error: (error) => {
          console.error('No se pudo crear el bloqueo temporal.', error);
          this.mensajeReserva.set(this.obtenerMensajeError(
            error,
            'No se pudo bloquear temporalmente la cabaña. Vuelve a consultar las fechas.'
          ));
          this.creandoReserva.set(false);
          this.consultarDisponibilidad();
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
    if (this.rangoNoDisponible(dia.fecha)) return `${fecha}, no disponible`;
    if (this.esDiaLimite(dia.fecha)) return `${fecha}, fecha seleccionada`;
    if (this.estaEnEstadia(dia.fecha)) return `${fecha}, dentro de tu estancia`;

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

  private obtenerMensajeError(error: any, mensajePredeterminado: string): string {
    if (error?.status === 0) {
      return 'No pudimos conectarnos con el servicio de reservas. Intenta nuevamente.';
    }

    if (error?.status === 401) {
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
