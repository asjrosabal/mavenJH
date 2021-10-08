package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Especialista;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data SQL reactive repository for the Especialista entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EspecialistaRepository extends R2dbcRepository<Especialista, Long>, EspecialistaRepositoryInternal {
    // just to avoid having unambigous methods
    @Override
    Flux<Especialista> findAll();

    @Override
    Mono<Especialista> findById(Long id);

    @Override
    <S extends Especialista> Mono<S> save(S entity);
}

interface EspecialistaRepositoryInternal {
    <S extends Especialista> Mono<S> insert(S entity);
    <S extends Especialista> Mono<S> save(S entity);
    Mono<Integer> update(Especialista entity);

    Flux<Especialista> findAll();
    Mono<Especialista> findById(Long id);
    Flux<Especialista> findAllBy(Pageable pageable);
    Flux<Especialista> findAllBy(Pageable pageable, Criteria criteria);
}
