package pe.edu.upeu.soldeshiran.reservas_service.model;
@jakarta.persistence.Entity
@jakarta.persistence.Table(name="cerrojo_reservas")
public class CerrojoReservas {
 @jakarta.persistence.Id private Long id;
 @jakarta.persistence.Column(name="ultimo_codigo", columnDefinition="bigint default 0")
 private Long ultimoCodigo = 0L;
}
