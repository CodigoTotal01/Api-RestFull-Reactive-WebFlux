package com.bolsadeideas.springboot.webflux.app;

import com.bolsadeideas.springboot.webflux.app.handler.ProductoHandler;
import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;


import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class RouterFunctionConfig {


    @Autowired
    private ProductoService service;



    @Bean
    public RouterFunction<ServerResponse> routes(ProductoHandler handler) {

        return route(GET("/api/v2/productos").or(GET("/api/v3/productos")), handler::listar)
                //and - para colocar el contenttype del request - si ahce match concordara con la respuesta -wen la bacera se edene enviar esta ifnormacion - request
                .andRoute(GET("/api/v2/productos/{id}"), handler::ver)
                .andRoute(POST("/api/v2/productos"), handler::crear)
                .andRoute(POST("/api/v2/productos/crear"), handler::crearProductoConFoto)
                .andRoute(POST("/api/v2/productos/upload/{id}"), handler::upload)
                .andRoute(PUT("/api/v2/productos/{id}"), handler::editar)
                .andRoute(DELETE("/api/v2/productos/{id}"), handler::eliminar);
    }




}

