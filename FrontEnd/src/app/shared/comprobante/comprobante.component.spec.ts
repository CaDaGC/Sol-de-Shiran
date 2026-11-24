import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComprobanteComponent } from './comprobante.component';
describe('Comprobante PDF generado en backend',()=>{
  let http:HttpTestingController;
  const crearUrl=URL.createObjectURL,revocar=URL.revokeObjectURL;
  const pickerOriginal=Object.getOwnPropertyDescriptor(window,'showSaveFilePicker');
  beforeEach(()=>{
    TestBed.configureTestingModule({imports:[ComprobanteComponent],providers:[provideHttpClient(),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);
    URL.createObjectURL=vi.fn().mockReturnValue('blob:http://localhost/comprobante');URL.revokeObjectURL=vi.fn();
  });
  afterEach(()=>{http.verify();URL.createObjectURL=crearUrl;URL.revokeObjectURL=revocar;
    if(pickerOriginal)Object.defineProperty(window,'showSaveFilePicker',pickerOriginal);else Reflect.deleteProperty(window,'showSaveFilePicker');});
  function crear(){const f=TestBed.createComponent(ComprobanteComponent);f.componentRef.setInput('idReserva',9);f.detectChanges();
    const dialog=f.nativeElement.querySelector('dialog') as HTMLDialogElement;dialog.showModal=()=>{dialog.open=true;};dialog.close=()=>{dialog.open=false;};return f;}
  async function abrir(f:ReturnType<typeof crear>){
    const promesa=f.componentInstance.abrir();http.expectOne(r=>r.method==='POST'&&r.url.endsWith('/reservas/9/comprobante')).flush({numero:'CP-00000007',nombreArchivo:'CP-00000007.pdf'});
    await Promise.resolve();const blob=new Blob(['%PDF-muestra'],{type:'application/pdf'});
    http.expectOne(r=>r.method==='GET'&&r.url.endsWith('/comprobante/pdf')).flush(blob);await promesa;f.detectChanges();return blob;
  }
  it('obtiene el PDF privado y permite elegir carpeta y nombre sin volver a generarlo',async()=>{
    const f=crear();const blob=await abrir(f);expect(f.nativeElement.querySelector('iframe')).not.toBeNull();
    const write=vi.fn().mockResolvedValue(undefined),close=vi.fn().mockResolvedValue(undefined);
    const picker=vi.fn().mockResolvedValue({createWritable:async()=>({write,close})});Object.defineProperty(window,'showSaveFilePicker',{configurable:true,value:picker});
    await f.componentInstance.descargar();expect(picker).toHaveBeenCalledWith(expect.objectContaining({suggestedName:'CP-00000007.pdf'}));
    expect(write).toHaveBeenCalledWith(blob);expect(close).toHaveBeenCalledOnce();http.expectNone(r=>r.method==='POST');
    f.componentInstance.cerrar();expect(URL.revokeObjectURL).toHaveBeenCalled();f.destroy();
  });
  it('cancelar el diálogo de guardado no inicia otra descarga',async()=>{
    const f=crear();await abrir(f);Object.defineProperty(window,'showSaveFilePicker',{configurable:true,value:vi.fn().mockRejectedValue(new DOMException('Cancelado','AbortError'))});
    await f.componentInstance.descargar();expect(f.componentInstance.error()).toBe('');expect(f.componentInstance.descargando()).toBe(false);f.destroy();
  });
  it('no muestra un PDF si el servidor rechaza el acceso',async()=>{
    const f=crear();const promise=f.componentInstance.abrir();http.expectOne(r=>r.method==='POST').flush({detail:'No autorizado'},{status:403,statusText:'Forbidden'});await promise;
    expect(f.componentInstance.vista()).toBeNull();expect(f.componentInstance.error()).toBe('No autorizado');http.expectNone(r=>r.method==='GET');f.destroy();
  });
});
