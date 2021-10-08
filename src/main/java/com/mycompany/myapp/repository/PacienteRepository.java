package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Paciente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data SQL reactive repository for the Paciente entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PacienteRepository extends R2dbcRepository<Paciente, Long>, PacienteRepositoryInternal {
    @Query("SELECT * FROM paciente entity WHERE entity.rut_id = :id")
    Flux<Paciente> findByRut(Long id);

    @Query("SELECT * FROM paciente entity WHERE entity.rut_id IS NULL")
    Flux<Paciente> findAllWhereRutIsNull();

    // just to avoid having unambigous methods
    @Override
    Flux<Paciente> findAll();

    @Override
    Mono<Paciente> findById(Long id);

    @Override
    <S extends Paciente> Mono<S> save(S entity);
}

interface PacienteRepositoryInternal {
    <S extends Paciente> Mono<S> insert(S entity);
    <S extends Paciente> Mono<S> save(S entity);
    Mono<Integer> update(Paciente entity);

    Flux<Paciente> findAll();
    Mono<Paciente> findById(Long id);
    Flux<Paciente> findAllBy(Pageable pageable);
    Flux<Paciente> findAllBy(Pageable pageable, Criteria criteria);
}
