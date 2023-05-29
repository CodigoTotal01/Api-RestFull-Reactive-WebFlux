package com.bolsadeideas.springboot.webflux.app;

import com.bolsadeideas.springboot.webflux.app.models.documents.Categoria;
import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;

import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

// trabajara con aplicaction, la clase principal y trabajar con el contexto de string
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // arracca siempre en un puerto aleatorio, evita conflictos
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // simulado o falso el servidor y all
class SpringBootWebfluxApirestApplicationTests {


    @Autowired
    private WebTestClient client;

    @Autowired
    private ProductoService service;

    @Value("${config.base.endpoint}")
    private String url;

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
                    Assertions.assertThat(productos.size() == 8).isTrue();
                });
    }
    @Test
    @DisplayName("Se debe obtener todos los datos del producto")
    void verDetalleProductoTest() {

        Producto productoMono = service.findByNombre("Galaxy fold 3v").block();
        //las pruebas unitarias no ttrabajan con subscribe, solo sincrono, no asyncrono
        client.get()
                .uri(url + "/{id}", Collections.singletonMap("id", productoMono.getId()))
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


    @Test
    public void creatTest(){
        //Block para que nos entrege el objeto un flujo de manera directa
        Categoria categoria = service.findCategoriaByNombre("Muebles").block();
        Producto producto = new Producto("Mesa comedor", 100.00, categoria);
        client.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON_UTF8) // tipo de datos que se envia
                .accept(MediaType.APPLICATION_JSON) //tipo de datos que retorna
                .body(Mono.just(producto), Producto.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                //json path
                .expectBody()
                .jsonPath("$.producto.id").isNotEmpty()
                .jsonPath("$.producto.nombre").isEqualTo("Mesa comedor")
                .jsonPath("$.producto.categoria.nombre").isEqualTo("Muebles");
    }


    @Test
    public void creat2Test(){
        //Block para que nos entrege el objeto un flujo de manera directa
        Categoria categoria = service.findCategoriaByNombre("Muebles").block();
        Producto producto = new Producto("Mesa comedor", 100.00, categoria);
        client.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON_UTF8) // tipo de datos que se envia
                .accept(MediaType.APPLICATION_JSON) //tipo de datos que retorna
                .body(Mono.just(producto), Producto.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                //json path
                //REtornamos u ntipo map de java
                .expectBody(new ParameterizedTypeReference<LinkedHashMap<String, Object>>() {})
                .consumeWith(response -> {
                    // Indicamos el bombre de la llave
                    Object objectResponse  = response.getResponseBody().get("producto");
                    Producto productoResponse = new ObjectMapper().convertValue(objectResponse, Producto.class);
                    Assertions.assertThat(productoResponse.getId()).isNotEmpty();
                    Assertions.assertThat(productoResponse.getNombre()).isEqualTo("Mesa comedor");
                    Assertions.assertThat(productoResponse.getCategoria().getNombre()).isNotEmpty();
                });
    }


    @Test
    public void editarTest(){
        Producto producto = service.findByNombre("Play Station 3").block();
        Categoria categoria = service.findCategoriaByNombre("Electronico").block();

        Producto productoEditado = new Producto("Play Station 4", 1000.00, categoria);


        client.put().uri(url + "/{id}", Collections.singletonMap("id", producto.getId()))
                .contentType(MediaType.APPLICATION_JSON) // tipo de datos que se envia
                .accept(MediaType.APPLICATION_JSON) //tipo de datos que retorna
                .body(Mono.just(productoEditado), Producto.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.nombre").isEqualTo("Play Station 4")
                .jsonPath("$.categoria.nombre").isEqualTo("Electronico");
    }



    @Test
    public void elimiarTest(){
        Producto producto  = service.findByNombre("Play Station 3").block();
        client.delete()
                .uri(url + "/{id}", Collections.singletonMap("id", producto.getId()))
                .exchange()
                .expectStatus().isNoContent()
                .expectBody()
                .isEmpty();


        client.get()
                .uri("/api/v2/productos/{id}", Collections.singletonMap("id", producto.getId()))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .isEmpty();

    }
}
