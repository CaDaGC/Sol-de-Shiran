import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../../services/auth';
import { SidebarComponent } from './sidebar.component';

describe('Submenú de Gestión', () => {
  const perfil = signal({ nombre: 'Admin', email: 'admin@example.com', rol: 'ADMIN' });

  beforeEach(() => {
    perfil.set({ nombre: 'Admin', email: 'admin@example.com', rol: 'ADMIN' });
    TestBed.configureTestingModule({
      imports: [SidebarComponent],
      providers: [
        provideRouter([{ path: 'gestion/restaurantes', children: [] }, { path: 'gestion', children: [] }]),
        { provide: AuthService, useValue: { usuarioActual: perfil, fotoPerfil: signal(null), cargarFotoPerfil: vi.fn() } },
      ],
    });
  });

  it('abre y cierra Gestión sin cerrar el menú móvil y sincroniza los enlaces por sección', async () => {
    const f = TestBed.createComponent(SidebarComponent);
    f.detectChanges();
    f.componentInstance.menuMovilAbierto.set(true);
    const boton = f.nativeElement.querySelector('[aria-controls="gestion-submenu"]') as HTMLButtonElement;
    boton.click(); f.detectChanges();
    expect(boton.getAttribute('aria-expanded')).toBe('true');
    expect(f.componentInstance.menuMovilAbierto()).toBe(true);
    expect(f.nativeElement.querySelectorAll('#gestion-submenu a')).toHaveLength(8);
    await TestBed.inject(Router).navigateByUrl('/gestion?seccion=cuentas'); f.detectChanges();
    expect(f.componentInstance.seccionGestion()).toBe('cuentas');
    expect(f.nativeElement.querySelector('#gestion-submenu a.active').textContent.trim()).toBe('Usuarios');
    boton.click(); f.detectChanges(); expect(f.nativeElement.querySelector('#gestion-submenu').hidden).toBe(true);
    f.destroy();
  });

  it('personal de restaurante ve pedidos y perfil sin gestión administrativa', () => {
    perfil.update(p => ({ ...p, rol: 'RESTAURANTE' }));
    const f = TestBed.createComponent(SidebarComponent); f.detectChanges();
    expect(f.nativeElement.querySelector('a[href="/restaurante/pedidos"]')).not.toBeNull();
    expect(f.nativeElement.querySelectorAll('#gestion-submenu a')).toHaveLength(1);
    expect(f.nativeElement.querySelector('a[href="/guardian/reservas"]')).toBeNull();
    expect(f.nativeElement.querySelector('a[href="/catalogo"]')).toBeNull();
    f.destroy();
  });

  it('trabajador solo ve Mi perfil y al pulsar Gestión contraída solicita expandir el sidebar', () => {
    perfil.update(p => ({ ...p, rol: 'TRABAJADOR' }));
    const f = TestBed.createComponent(SidebarComponent);
    f.componentRef.setInput('contraido', true);
    const expandir = vi.fn(); f.componentInstance.alternar.subscribe(expandir); f.detectChanges();
    f.nativeElement.querySelector('[aria-controls="gestion-submenu"]').click(); f.detectChanges();
    expect(expandir).toHaveBeenCalledOnce();
    const enlaces = f.nativeElement.querySelectorAll('#gestion-submenu a');
    expect(enlaces).toHaveLength(1); expect(enlaces[0].textContent.trim()).toBe('Mi perfil');
    f.destroy();
  });
});
