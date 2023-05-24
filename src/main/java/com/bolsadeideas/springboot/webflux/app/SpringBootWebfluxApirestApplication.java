package com.bolsadeideas.springboot.webflux.app;

import com.bolsadeideas.springboot.webflux.app.models.documents.Categoria;
import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;

import java.util.Date;

@SpringBootApplication
public class SpringBootWebfluxApirestApplication implements CommandLineRunner {

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @Autowired
    private ProductoService service;

    private static final Logger log = LoggerFactory.getLogger(SpringBootWebfluxApirestApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootWebfluxApirestApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        // Eliminar la tabla - productos
        mongoTemplate.dropCollection("productos").subscribe();
        mongoTemplate.dropCollection("categorias").subscribe();


        //Categorias
        Categoria electronico = new Categoria("Electronico");
        Categoria deporte = new Categoria("Deporte");
        Categoria computacion = new Categoria("Computacion");
        Categoria muebles = new Categoria("Muebles");



        Flux.just(electronico, deporte, computacion, muebles)
                //            service.saveCategoria(categoria);
                .flatMap(service::saveCategoria)
                .doOnNext(categoria -> {
                    log.info("Categoria Creada: " +categoria.getNombre() + ", Id: ", categoria.getId());
                })
                //! Ejecutar otro flujo de datos despues
                .thenMany(

                        Flux.just(new Producto("TV Panasonic Pantalla LCD", 456.89, electronico),
                                        new Producto("Sony camara HD digital", 786.23, electronico),
                                        new Producto("Play Station 3", 700.00, electronico),
                                        new Producto("Laptop lenovo decima generacion", 5000.00, computacion),
                                        new Producto("Disco duro externo de 5T", 689.97, computacion),
                                        new Producto("Teclado mecanico redragon", 350.87, computacion),
                                        new Producto("Galaxy fold 3v", 3500.99, electronico)
                                )
                                //Asi retornara nadamas (save) un flujo de mono - osea otro flujo
                                .flatMap(producto -> {
                                    producto.setCreateAt(new Date());
                                    return service.save(producto);
                                })
                        // no tenemos el objeto en si, si no el mono, en estos casos el flatmap obtiene el obserbable y lo aplana a nuestro objeto deseado

                )
                .subscribe(producto -> log.info("Insert: " + producto.getId() + " " + producto.getNombre()));
    }

}
