export interface Persona {
  id?: number;
  nombres: string;
  apellidos: string;
  tipoDocumento: string;
  numeroDocumento: string;
  telefono: string;
  direccion: string;
}
export interface Cuenta {
  id: number;
  nombre: string;
  nombreUsuario: string;
  email: string;
  rol: string;
  activo: boolean;
  fotoPerfil: string | null;
  persona: Persona | null;
}
export const nuevaPersona = (): Persona => ({ nombres: '', apellidos: '', tipoDocumento: 'DNI',
  numeroDocumento: '', telefono: '', direccion: '' });
