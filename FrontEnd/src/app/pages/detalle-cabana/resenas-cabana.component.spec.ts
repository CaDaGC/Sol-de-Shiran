import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ResenasCabanaComponent } from './resenas-cabana.component';
import { AuthService } from '../../services/auth';
import { API_CONFIG } from '../../core/api.config';
describe('Alias y foto en reseñas',()=>{
  it('muestra alias público y URL del gateway; sin foto muestra iniciales',async()=>{
    TestBed.configureTestingModule({imports:[ResenasCabanaComponent],providers:[provideHttpClient(),provideHttpClientTesting(),{provide:AuthService,useValue:{usuarioActual:signal(null)}}]});
    const http=TestBed.inject(HttpTestingController);const f=TestBed.createComponent(ResenasCabanaComponent);f.componentRef.setInput('idCabana',3);f.detectChanges();
    http.expectOne(r=>r.url.endsWith('/resenas/cabanas/3')).flush({opiniones:[
      {id:1,autor:'Nombre anterior',nombreUsuario:'alias',fotoPerfil:'/api/v1/usuarios/fotos/foto.png',puntuacion:5,comentario:'Excelente'},
      {id:2,autor:'Otro',nombreUsuario:'lucia',fotoPerfil:null,puntuacion:4,comentario:'Muy bien'}],hayMas:false});
    f.detectChanges();await f.whenStable();f.detectChanges();
    const items=f.nativeElement.querySelectorAll('.reviews-list li');expect(items[0].textContent).toContain('alias');expect(items[0].textContent).not.toContain('Nombre anterior');
    expect(items[0].querySelector('img').getAttribute('src')).toBe(new URL(API_CONFIG.baseUrl).origin+'/api/v1/usuarios/fotos/foto.png');
    expect(items[1].querySelector('.review-avatar').textContent.trim()).toBe('L');
    items[0].querySelector('img').dispatchEvent(new Event('error'));f.detectChanges();expect(items[0].querySelector('.review-avatar').textContent.trim()).toBe('A');
    http.verify();f.destroy();
  });
});
