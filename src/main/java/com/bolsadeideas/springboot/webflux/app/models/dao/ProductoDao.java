package com.bolsadeideas.springboot.webflux.app.models.dao;

import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
//Ya no es jpa, si no, reactive - ya que pose objetos mono
//por defecto es un componente
public interface ProductoDao extends ReactiveMongoRepository<Producto, String> {

}
