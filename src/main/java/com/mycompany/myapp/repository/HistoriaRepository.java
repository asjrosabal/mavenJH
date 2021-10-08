package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Historia;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data SQL reactive repository for the Historia entity.
 */
@SuppressWarnings("unused")
@Repository
public interface HistoriaRepository extends R2dbcRepository<Historia, Long>, HistoriaRepositoryInternal {
    @Query("SELECT * FROM historia entity WHERE entity.rut_id = :id")
    Flux<Historia> findByRut(Long id);

    @Query("SELECT * FROM historia entity WHERE entity.rut_id IS NULL")
    Flux<Historia> findAllWhereRutIsNull();

    // just to avoid having unambigous methods
    @Override
    Flux<Historia> findAll();

    @Override
    Mono<Historia> findById(Long id);

    @Override
    <S extends Historia> Mono<S> save(S entity);
}

interface HistoriaRepositoryInternal {
    <S extends Historia> Mono<S> insert(S entity);
    <S extends Historia> Mono<S> save(S entity);
    Mono<Integer> update(Historia entity);

    Flux<Historia> findAll();
    Mono<Historia> findById(Long id);
    Flux<Historia> findAllBy(Pageable pageable);
    Flux<Historia> findAllBy(Pageable pageable, Criteria criteria);
}
