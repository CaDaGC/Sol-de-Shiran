import { Component, ElementRef, OnDestroy, inject, input, signal, viewChild } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';
import { API_CONFIG } from '../../core/api.config';
interface ArchivoGuardable { createWritable(): Promise<{ write(datos: Blob): Promise<void>; close(): Promise<void> }> }
type VentanaGuardado = Window & { showSaveFilePicker?: (opciones: unknown) => Promise<ArchivoGuardable> };
@Component({selector:'app-comprobante',standalone:true,templateUrl:'./comprobante.component.html',styleUrl:'./comprobante.component.css'})
export class ComprobanteComponent implements OnDestroy {
  readonly idReserva=input.required<number>();
  private readonly http=inject(HttpClient);
  private readonly sanitizer=inject(DomSanitizer);
  private readonly dialog=viewChild.required<ElementRef<HTMLDialogElement>>('visor');
  readonly cargando=signal(false);readonly descargando=signal(false);readonly error=signal('');
  readonly vista=signal<SafeResourceUrl|null>(null);readonly nombre=signal('comprobante.pdf');
  private url:string|null=null;private archivo:Blob|null=null;private destruido=false;private solicitud=0;
  async abrir():Promise<void>{
    if(this.cargando())return;
    this.error.set('');this.cargando.set(true);this.liberar();const solicitud=++this.solicitud;
    if(!this.dialog().nativeElement.open)this.dialog().nativeElement.showModal();
    const base=API_CONFIG.baseUrl+'/pagos/reservas/'+this.idReserva()+'/comprobante';
    try{
      const datos=await firstValueFrom(this.http.post<{nombreArchivo:string}>(base,{}));
      const archivo=await firstValueFrom(this.http.get(base+'/pdf',{responseType:'blob'}));
      if(this.destruido||solicitud!==this.solicitud)return;
      this.nombre.set(datos.nombreArchivo);this.archivo=archivo;
      this.url=URL.createObjectURL(archivo);this.vista.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.url));
    }catch(e){if(!this.destruido&&solicitud===this.solicitud)this.error.set(e instanceof HttpErrorResponse
      ? e.error?.detail ?? (e.status===404?'No hay un comprobante de adelanto disponible para esta reserva.':'No se pudo obtener el comprobante. Reintenta sin volver a pagar.')
      :'No se pudo abrir el comprobante.');}
    finally{if(!this.destruido&&solicitud===this.solicitud)this.cargando.set(false);}
  }
  async descargar():Promise<void>{
    if(!this.archivo||this.descargando())return;
    this.descargando.set(true);this.error.set('');
    try{
      const ventana=window as VentanaGuardado;
      if(ventana.showSaveFilePicker){
        const destino=await ventana.showSaveFilePicker({suggestedName:this.nombre(),types:[{description:'Documento PDF',accept:{'application/pdf':['.pdf']}}]});
        const salida=await destino.createWritable();await salida.write(this.archivo);await salida.close();
      }else{
        const enlace=document.createElement('a');enlace.href=this.url!;enlace.download=this.nombre();document.body.appendChild(enlace);enlace.click();enlace.remove();
      }
    }catch(e){if(!(e instanceof DOMException&&e.name==='AbortError'))this.error.set('No se pudo guardar el archivo. Vuelve a intentarlo.');}
    finally{this.descargando.set(false);}
  }
  cerrar():void{if(this.descargando())return;this.solicitud++;this.dialog().nativeElement.close();this.cargando.set(false);this.liberar();}

  private liberar():void{this.vista.set(null);this.archivo=null;if(this.url)URL.revokeObjectURL(this.url);this.url=null;}
  ngOnDestroy():void{this.destruido=true;this.solicitud++;this.liberar();}
}
