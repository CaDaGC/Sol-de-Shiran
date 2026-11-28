import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { inicioPorRol, AuthService } from '../../services/auth';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
})
export class SidebarComponent {
  readonly inicioPorRol = inicioPorRol;
  readonly contraido = input(false);
  readonly menuMovilAbierto = signal(false);
  readonly alternar = output<void>();
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly gestionAbierta = signal(false);
  readonly seccionGestion = signal('');
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.actualizarGestion();
    this.router.events.pipe(filter(event => event instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.actualizarGestion());
  }
  private actualizarGestion(): void {
    const ruta = this.router.parseUrl(this.router.url);
    const enGestion = ruta.root.children['primary']?.segments[0]?.path === 'gestion';
    const restaurantes = ruta.root.children['primary']?.segments[1]?.path === 'restaurantes';
    this.seccionGestion.set(enGestion ? ruta.queryParams['seccion'] ?? (restaurantes ? 'restaurantes' : this.perfil()?.rol === 'ADMIN' ? 'cabanas' : 'perfil') : '');
    if (enGestion) this.gestionAbierta.set(true);
  }
  alternarGestion(evento: MouseEvent): void {
    evento.stopPropagation();
    if (this.contraido()) {
      this.alternar.emit();
      this.gestionAbierta.set(true);
    } else this.gestionAbierta.update(abierta => !abierta);
  }
  readonly perfil = this.authService.usuarioActual;
  readonly fotoPerfil = this.authService.fotoPerfil;

  ngOnInit(): void {
    this.authService.cargarFotoPerfil();
  }

  cerrarSesion(): void {
    this.authService.cerrarSesion();
    void this.router.navigate(['/login']);
  }
}
