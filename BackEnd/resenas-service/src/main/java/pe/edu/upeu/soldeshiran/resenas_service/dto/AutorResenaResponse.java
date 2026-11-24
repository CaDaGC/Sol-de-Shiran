package pe.edu.upeu.soldeshiran.resenas_service.dto;
public record AutorResenaResponse(Long id, String nombre, String nombreUsuario, String fotoPerfil) {
  public AutorResenaResponse(Long id,String nombre) {this(id,nombre,nombre,null);}
}
