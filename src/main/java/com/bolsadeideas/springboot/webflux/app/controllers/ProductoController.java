package com.bolsadeideas.springboot.webflux.app.controllers;

import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.swing.text.html.parser.Entity;
import javax.validation.Valid;
import java.io.File;
import java.net.URI;
import java.util.*;


@RestController //para trabajar con metodos restfull - la media type sera json por defecto
@RequestMapping("/api/productos") // ruta base
public class ProductoController {

    @Autowired
    private ProductoService service;

    @Value("${config.uploads.path}")
    private String path;


    //la info deve ser form data

    @PostMapping("/v2")
    public Mono<ResponseEntity<Producto>> crearConFoto(Producto producto, @RequestPart("file") FilePart file) {
        if (producto.getCreateAt() == null) {
            producto.setCreateAt(new Date());
        }

        producto.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                .replace(" ", "")
                .replace(":", "")
                .replace("\\", ""));


        return file.transferTo(new File(path + producto.getFoto()))
                .then(service.save(producto))
                .map(nuevoProducto ->
                        ResponseEntity.created(URI.create("/api/productos/".concat(nuevoProducto.getId())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(nuevoProducto)
                );

    }


    @PostMapping("/upload/{id}")
    public Mono<ResponseEntity<Producto>> upload(@PathVariable String id, @RequestPart("file") FilePart file) {
        return service.findById(id).flatMap(producto -> {
            producto.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                    .replace(" ", "")
                    .replace(":", "")
                    .replace("\\", ""));
            return file.transferTo(new File(path + producto.getFoto()))
                    .then(service.save(producto));
        }).map(producto -> ResponseEntity.ok(producto)).defaultIfEmpty(ResponseEntity.notFound().build());
    }


    @GetMapping
    public Mono<ResponseEntity<Flux<Producto>>> lista() {
        // Mono es para un solo flujo de datos
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.findAll()));
    }


    @GetMapping("/{id}")
    public Mono<ResponseEntity<Producto>> ver(@PathVariable String id) {
        return service.findById(id)
                .map(producto -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(producto))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    //@RequestBody - se envia por el body el producto - un json
    /*
    @PostMapping
    public Mono<ResponseEntity<Producto>> crear(@RequestBody Producto producto) {
        if (producto.getCreateAt() == null) {
            producto.setCreateAt(new Date());
        }
        return service.save(producto)
                .map(nuevoProducto ->
                        //url del detalle del producto que se acaba de crear
                        ResponseEntity.created(URI.create("/api/productos/".concat(nuevoProducto.getId())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(nuevoProducto)
                );
    }
    */
    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> crear(@Valid @RequestBody Mono<Producto> monoProducto){

        Map<String, Object> respuesta = new HashMap<String, Object>();

        return monoProducto.flatMap(producto -> {
            if(producto.getCreateAt()==null) {
                producto.setCreateAt(new Date());
            }

            return service.save(producto).map(p-> {
                respuesta.put("producto", p);
                respuesta.put("mensaje", "Producto creado con éxito");
                respuesta.put("timestamp", new Date());
                return ResponseEntity
                        .created(URI.create("/api/productos/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(respuesta);
            });

        }).onErrorResume(t -> {
            return Mono.just(t).cast(WebExchangeBindException.class)
                    .flatMap(e -> Mono.just(e.getFieldErrors()))
                    .flatMapMany(Flux::fromIterable)
                    .map(fieldError -> "El campo "+fieldError.getField() + " " + fieldError.getDefaultMessage())
                    .collectList()
                    .flatMap(list -> {
                        respuesta.put("errors", list);
                        respuesta.put("timestamp", new Date());
                        respuesta.put("status", HttpStatus.BAD_REQUEST.value());
                        return Mono.just(ResponseEntity.badRequest().body(respuesta));
                    });

        });


    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Producto>> editar(@RequestBody Producto producto, @PathVariable String id) {
        return service.findById(id)
                //flatmap cuando necesitemos modificar nuestro flujo original por uno nuevo
                .flatMap(productoEmitido -> {
                    productoEmitido.setNombre(producto.getNombre());
                    productoEmitido.setPrecio(producto.getPrecio());
                    productoEmitido.setCategoria(producto.getCategoria());
                    return service.save(productoEmitido);
                })
                // Map cuando siemplemente atravez del mismo flujo retornar otro distinto
                .map(productoActualizado -> ResponseEntity
                        .created(URI.create("/api/productos/".concat(productoActualizado.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(productoActualizado))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

//    @DeleteMapping("/{id}")
//    public Mono<ResponseEntity<Void>> eliminar (@PathVariable String id){
//        return service.findById(id).flatMap(producto -> {
//            return service.delete(producto)
//                    .then(Mono.just(ResponseEntity.noContent().build()));
//        });
//    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> eliminar(@PathVariable String id) {
        return service.findById(id).flatMap(producto -> {
            return service.delete(producto)
                    .then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)));
        }).defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
    }


}
