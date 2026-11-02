export interface Cabana {
  idCabana: number;
  nombre: string;
  descripcion: string;
  aforoMaximo: number;
  numHabitaciones: number;
  numBanos: number;
  tarifaNoche: number;
  imagenPrincipal: string;
}

export interface CabanaDetalle extends Cabana {
  imagenesSecundarias: string[];
}
