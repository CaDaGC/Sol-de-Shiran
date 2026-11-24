import { DOCUMENT } from '@angular/common';
import { Router } from '@angular/router';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth';
@Component({
  selector: 'app-public-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './public-navbar.component.html',
  styleUrl: './public-navbar.component.css',
})
export class PublicNavbarComponent {
  private readonly documento = inject(DOCUMENT);
  private readonly router = inject(Router);
  irASeccion(evento: Event, seccion: string): void {
    if (this.router.url.split(/[?#]/)[0] !== '/catalogo') return;
    const destino = this.documento.getElementById(seccion);
    if (!destino) return;
    evento.preventDefault();
    destino.scrollIntoView({ block: 'start' });
  }
  readonly usuario = inject(AuthService).usuarioActual;
}
