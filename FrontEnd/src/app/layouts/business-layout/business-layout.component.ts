import { afterNextRender, Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';

@Component({
  selector: 'app-business-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent],
  templateUrl: './business-layout.component.html',
  styleUrl: './business-layout.component.css'
})
export class BusinessLayoutComponent {
  readonly contraido = signal(false);

  constructor() {
    afterNextRender(() => {
      try {
        this.contraido.set(localStorage.getItem('sidebar-contraido') === 'true');
      } catch {
        // El menú funciona aunque el navegador bloquee el almacenamiento local.
      }
    });
  }

  alternarSidebar(): void {
    this.contraido.update(valor => !valor);
    try {
      localStorage.setItem('sidebar-contraido', String(this.contraido()));
    } catch {
      // Conserva la preferencia durante la navegación de esta sesión.
    }
  }
}
