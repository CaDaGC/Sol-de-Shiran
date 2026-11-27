package pe.edu.upeu.soldeshiran.restaurante_service.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.CategoriaMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.ProductoMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.mapper.CartaMenuMapper;
import pe.edu.upeu.soldeshiran.restaurante_service.model.TipoCategoria;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.CategoriaMenuRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.ProductoMenuRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.CartaMenuService;
import pe.edu.upeu.soldeshiran.restaurante_service.service.RestauranteConsultaService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartaMenuServiceImpl implements CartaMenuService {

    private final RestauranteConsultaService restaurantes;
    private final CategoriaMenuRepository categorias;
    private final ProductoMenuRepository productos;
    private final CartaMenuMapper mapper;

    @Override
    public List<CategoriaMenuResponse> consultarCategorias(
            Long idRestaurante,
            Long idReserva,
            Actor actor) {

        restaurantes.seleccionarRestaurante(
                idRestaurante,
                idReserva,
                actor
        );

        return categorias
                .findByRestauranteIdRestauranteOrderByTipoAscNombreAscIdCategoriaAsc(
                        idRestaurante
                )
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<ProductoMenuResponse> consultarProductos(
            Long idRestaurante,
            Long idReserva,
            Long idCategoria,
            TipoCategoria tipo,
            Actor actor) {

        restaurantes.seleccionarRestaurante(
                idRestaurante,
                idReserva,
                actor
        );

        if (idCategoria != null) {
            if (idCategoria <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La categoría debe ser válida."
                );
            }

            categorias
                    .findByIdCategoriaAndRestauranteIdRestaurante(
                            idCategoria,
                            idRestaurante
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "La categoría no pertenece al restaurante seleccionado."
                    ));
        }

        return productos.consultarCarta(
                        idRestaurante,
                        idCategoria,
                        tipo
                )
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}