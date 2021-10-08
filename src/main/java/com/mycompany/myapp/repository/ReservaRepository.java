package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Reserva;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data SQL reactive repository for the Reserva entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ReservaRepository extends R2dbcRepository<Reserva, Long>, ReservaRepositoryInternal {
    @Query("SELECT * FROM reserva entity WHERE entity.rut_id = :id")
    Flux<Reserva> findByRut(Long id);

    @Query("SELECT * FROM reserva entity WHERE entity.rut_id IS NULL")
    Flux<Reserva> findAllWhereRutIsNull();

    // just to avoid having unambigous methods
    @Override
    Flux<Reserva> findAll();

    @Override
    Mono<Reserva> findById(Long id);

    @Override
    <S extends Reserva> Mono<S> save(S entity);
}

interface ReservaRepositoryInternal {
    <S extends Reserva> Mono<S> insert(S entity);
    <S extends Reserva> Mono<S> save(S entity);
    Mono<Integer> update(Reserva entity);

    Flux<Reserva> findAll();
    Mono<Reserva> findById(Long id);
    Flux<Reserva> findAllBy(Pageable pageable);
    Flux<Reserva> findAllBy(Pageable pageable, Criteria criteria);
}
