package pe.edu.upeu.soldeshiran.usuario_service.dto;
import pe.edu.upeu.soldeshiran.usuario_service.model.Usuario;
public record UsuarioResumenResponse(Long id, String nombre, String email,
    String nombreUsuario, String fotoPerfil, PersonaResponse persona) {
  public UsuarioResumenResponse(Long id, String nombre, String email) {
    this(id,nombre,email,nombre,null,null);
  }
  public static UsuarioResumenResponse desde(Usuario u) {
    var persona = new pe.edu.upeu.soldeshiran.usuario_service.mapper.PersonaMapper().toResponse(u.getPersona());
    return new UsuarioResumenResponse(u.getId(),u.getNombre(),u.getEmail(),u.getNombre(),u.getFotoPerfil(),persona);
  }
}
