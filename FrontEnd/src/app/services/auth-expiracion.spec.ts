import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from './auth';
const token=(iat:number,exp:number)=>'e30.'+btoa(JSON.stringify({iat,exp,userId:1,rol:'CLIENTE',email:'test@example.com'}))+'.firma';
function crearStorage(): Storage {
  const valores = new Map<string, string>();
  return {
    get length() { return valores.size; },
    clear: () => valores.clear(),
    getItem: (clave) => valores.get(clave) ?? null,
    key: (indice) => Array.from(valores.keys())[indice] ?? null,
    removeItem: (clave) => { valores.delete(clave); },
    setItem: (clave, valor) => { valores.set(clave, String(valor)); },
  };
}
describe('Expiración obligatoria de sesión',()=>{
  beforeEach(()=>{vi.stubGlobal('localStorage', crearStorage());vi.stubGlobal('sessionStorage', crearStorage());vi.useFakeTimers();vi.setSystemTime(new Date('2026-11-20T20:00:00Z'));TestBed.configureTestingModule({providers:[provideHttpClient(),provideRouter([])]});});
  afterEach(()=>{TestBed.inject(AuthService).cerrarSesion();localStorage.clear();sessionStorage.clear();vi.unstubAllGlobals();vi.useRealTimers();});
  it('expulsa al cumplirse una hora aunque el JWT antiguo anuncie 24 horas',async()=>{
    const ahora=Date.now()/1000;localStorage.setItem('token',token(ahora,ahora+86400));
    const navegar=vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);
    const auth=TestBed.inject(AuthService);expect(auth.estaAutenticado()).toBe(true);
    await vi.advanceTimersByTimeAsync(3600001);
    expect(localStorage.getItem('token')).toBeNull();expect(auth.usuarioActual()).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/login'],{queryParams:{sesion:'expirada'},replaceUrl:true});
    expect(sessionStorage.getItem('sesionExpirada')).toBe('true');
  });
  it('avisa al restaurar un token vencido sin mantener acceso',async()=>{
    const ahora=Date.now()/1000;localStorage.setItem('token',token(ahora-7200,ahora-3600));
    const navegar=vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);
    const auth=TestBed.inject(AuthService);await vi.runAllTicks();
    expect(auth.estaAutenticado()).toBe(false);expect(navegar).toHaveBeenCalled();
  });
});
