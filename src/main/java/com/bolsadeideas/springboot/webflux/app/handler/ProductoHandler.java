package com.bolsadeideas.springboot.webflux.app.handler;

import com.bolsadeideas.springboot.webflux.app.models.documents.Categoria;
import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.stereotype.Component;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;


//? Seria como nuestro controlador
@Component
public class ProductoHandler {

    @Autowired
    private ProductoService service;

    //! En los metodos hablndler no se peude aplicar @Validation, asi que toca de manera manual
    @Autowired
    private Validator validator; // Typo error - binding results

    @Value("${config.uploads.path}")
    private String path;

    public Mono<ServerResponse> upload(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.multipartData()
                .map(multipart -> multipart.toSingleValueMap().get("file"))
                .cast(FilePart.class)
                .flatMap(filePart -> service.findById(id).flatMap(producto -> {
                    producto.setFoto(UUID.randomUUID().toString() + "-" + filePart.filename()
                            .replace(" ", "-")
                            .replace(",", "")
                            .replace("\\", "")
                    );

                    return filePart.transferTo(new File(path + producto.getFoto())).then(service.save(producto));
                })).flatMap(producto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fromValue(producto)))
                .switchIfEmpty(ServerResponse.notFound().build());
    }


    public Mono<ServerResponse> crearProductoConFoto(ServerRequest request) {

        Mono<Producto> productoMono = request.multipartData()
                .map(multipart -> {
                    FormFieldPart nombre = (FormFieldPart) multipart.toSingleValueMap().get("nombre");
                    FormFieldPart precio = (FormFieldPart) multipart.toSingleValueMap().get("precio");
                    FormFieldPart categoriaId = (FormFieldPart) multipart.toSingleValueMap().get("categoria.id");
                    FormFieldPart categoriaNombre = (FormFieldPart) multipart.toSingleValueMap().get("categoria.nombre");

                    Categoria categoria = new Categoria(categoriaNombre.value());
                    categoria.setId(categoriaId.value());
                    System.out.println("-------");
                    System.out.println(categoria.getNombre());
                    System.out.println(categoria.getId());
                    //map nuncaretorna un nuevo flujo de datos
                    return new Producto(nombre.value(), Double.parseDouble(precio.value()), categoria);
                });

        return request.multipartData()
                .map(multipart -> multipart.toSingleValueMap().get("file"))
                .cast(FilePart.class)
                .flatMap(filePart -> productoMono
                        .flatMap(producto -> {
                            producto.setFoto(UUID.randomUUID().toString() + "-" + filePart.filename()
                                    .replace(" ", "-")
                                    .replace(",", "")
                                    .replace("\\", "")
                            );


                            producto.setCreateAt(new Date());

                            return filePart
                                    .transferTo(new File(path + producto.getFoto()))
                                    .then(service.save(producto));
                        })).flatMap(producto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fromValue(producto)))
                ;
    }

    public Mono<ServerResponse> listar(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(service.findAll(), Producto.class);
    }

    public Mono<ServerResponse> ver(ServerRequest request) {
        //obteniendo id por el request
        String id = request.pathVariable("id");
        //para convertir de un tipo de terminado ade pflujo a otro usar flat map
        return service.findById(id).flatMap(producto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        //no es untipo reactivo el que se emite por esto este cambiaso
                        //fromObject esta deprecated
                        .body(fromValue(producto)))
                //basicamente isi ta basio el flujo
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> crear(ServerRequest request) {
        Mono<Producto> productoMono = request.bodyToMono(Producto.class);
        //algo ratro de flat map es que accede al flujo de datos directamente a los elementos, en este caso al bojeto producto
        return productoMono.flatMap(producto -> {

            Errors errors = new BeanPropertyBindingResult(producto, Producto.class.getName());
            validator.validate(producto, errors);


            if (errors.hasErrors()) {
                return Flux.fromIterable(errors.getFieldErrors())
                        .map(
                                //iteral poc cada elemento para convertiolo a otro flux
                                fieldError -> "El cambo " + fieldError.getField() + " " + fieldError.getDefaultMessage())
                        .collectList()
                        .flatMap(lista ->
                                ServerResponse.badRequest()
                                        .body(fromValue(lista)));

            } else {
                if (producto.getCreateAt() == null) {
                    producto.setCreateAt(new Date());
                }

                return service.save(producto).flatMap(productoDB ->
                        ServerResponse.created(URI.create("/api/v2/productos".concat(productoDB.getId())))
                                .contentType(MediaType.APPLICATION_JSON).body(fromValue(productoDB))
                );
            }

        });
    }

    public Mono<ServerResponse> editar(ServerRequest request) {
        Mono<Producto> productoActual = request.bodyToMono(Producto.class);
        String id = request.pathVariable("id");

        Mono<Producto> productoAnteriorDB = service.findById(id);
        //producto actualizado optione los valores del producto anterior, combinar dos productos mno
        return productoAnteriorDB.zipWith(productoActual, (productoAnterior, requestProductoActual) -> {
            productoAnterior.setNombre(requestProductoActual.getNombre());
            productoAnterior.setPrecio(requestProductoActual.getPrecio());
            productoAnterior.setCategoria(requestProductoActual.getCategoria());
            return productoAnterior;
        }).flatMap(productoActulizado -> {
            return ServerResponse.created(URI.create("/api/v2/productos".concat(productoActulizado.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    //lo acepta porque es un tipo reactivo
                    .body(service.save(productoActulizado), Producto.class);
        }).switchIfEmpty(ServerResponse.notFound().build());

    }


    public Mono<ServerResponse> eliminar(ServerRequest request) {
        String id = request.pathVariable("id");
        Mono<Producto> productoAnteriorDB = service.findById(id);

        //siempre sera vacio
        return productoAnteriorDB.flatMap(producto -> service
                        .delete(producto)
                        .then(ServerResponse.noContent().build()))
                .switchIfEmpty(ServerResponse.notFound().build());


    }

}
