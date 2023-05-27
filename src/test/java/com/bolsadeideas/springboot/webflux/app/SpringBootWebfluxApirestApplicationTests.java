package com.bolsadeideas.springboot.webflux.app;

import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;

import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

// trabajara con aplicaction, la clase principal y trabajar con el contexto de string
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // arracca siempre en un puerto aleatorio, evita conflictos
class SpringBootWebfluxApirestApplicationTests {


    @Autowired
    private WebTestClient client;

    @Autowired
    private ProductoService service;

    @Test
    @DisplayName("La lista de productos no debe ser menor ni mayor a 9")
    void listarTest() {
        client.get()
                .uri("/api/v2/productos")
                .accept(MediaType.APPLICATION_JSON) // cabezera
                .exchange() // enviar request y consumirlo
                .expectStatus().isOk() //tipo de respuesta que esperamos
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(Producto.class)     // espera una lista de productos
//                .hasSize(7);
                .consumeWith(response -> {
                    List<Producto> productos = response.getResponseBody();
                    productos.forEach(producto -> {
                        System.out.println(producto.getNombre());
                    });
                    //Para generar verificacion
                    Assertions.assertThat(productos.size() == 7).isTrue();
                });
    }
    @Test
    @DisplayName("Se debe obtener todos los datos del producto")
    void verDetalleProductoTest() {

        Producto productoMono = service.findByNombre("Galaxy fold 3v").block();
        //las pruebas unitarias no ttrabajan con subscribe, solo sincrono, no asyncrono
        client.get()
                .uri("/api/v2/productos/{id}", Collections.singletonMap("id", productoMono.getId()))
                //pasar variable del id, el problema que el id no es fijo
                .accept(MediaType.APPLICATION_JSON) // cabezera
                .exchange() // enviar request y consumirlo
                .expectStatus().isOk() //tipo de respuesta que esperamos
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Producto.class)
//                .jsonPath("$.id").isNotEmpty()
//                .jsonPath("$.nombre").isEqualTo("Galaxy fold 3v");
                .consumeWith(response -> {
                    Producto producto = response.getResponseBody();
                    //Para generar verificacion
                    Assertions.assertThat(producto.getId()).isNotEmpty();//objetos
                    Assertions.assertThat(producto.getId().length()>0).isTrue(); //boleanos
                    Assertions.assertThat(producto.getNombre()).isEqualTo("Galaxy fold 3v"); //string
                });

    }

}
