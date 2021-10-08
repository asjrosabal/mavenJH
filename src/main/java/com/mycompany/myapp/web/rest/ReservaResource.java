package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.Reserva;
import com.mycompany.myapp.repository.ReservaRepository;
import com.mycompany.myapp.repository.search.ReservaSearchRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing {@link com.mycompany.myapp.domain.Reserva}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class ReservaResource {

    private final Logger log = LoggerFactory.getLogger(ReservaResource.class);

    private static final String ENTITY_NAME = "mavenReserva";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ReservaRepository reservaRepository;

    private final ReservaSearchRepository reservaSearchRepository;

    public ReservaResource(ReservaRepository reservaRepository, ReservaSearchRepository reservaSearchRepository) {
        this.reservaRepository = reservaRepository;
        this.reservaSearchRepository = reservaSearchRepository;
    }

    /**
     * {@code POST  /reservas} : Create a new reserva.
     *
     * @param reserva the reserva to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new reserva, or with status {@code 400 (Bad Request)} if the reserva has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/reservas")
    public Mono<ResponseEntity<Reserva>> createReserva(@RequestBody Reserva reserva) throws URISyntaxException {
        log.debug("REST request to save Reserva : {}", reserva);
        if (reserva.getId() != null) {
            throw new BadRequestAlertException("A new reserva cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return reservaRepository
            .save(reserva)
            .flatMap(reservaSearchRepository::save)
            .map(
                result -> {
                    try {
                        return ResponseEntity
                            .created(new URI("/api/reservas/" + result.getId()))
                            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
    }

    /**
     * {@code PUT  /reservas/:id} : Updates an existing reserva.
     *
     * @param id the id of the reserva to save.
     * @param reserva the reserva to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated reserva,
     * or with status {@code 400 (Bad Request)} if the reserva is not valid,
     * or with status {@code 500 (Internal Server Error)} if the reserva couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/reservas/{id}")
    public Mono<ResponseEntity<Reserva>> updateReserva(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Reserva reserva
    ) throws URISyntaxException {
        log.debug("REST request to update Reserva : {}, {}", id, reserva);
        if (reserva.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, reserva.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return reservaRepository
            .existsById(id)
            .flatMap(
                exists -> {
                    if (!exists) {
                        return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                    }

                    return reservaRepository
                        .save(reserva)
                        .flatMap(reservaSearchRepository::save)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                        .map(
                            result ->
                                ResponseEntity
                                    .ok()
                                    .headers(
                                        HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString())
                                    )
                                    .body(result)
                        );
                }
            );
    }

    /**
     * {@code PATCH  /reservas/:id} : Partial updates given fields of an existing reserva, field will ignore if it is null
     *
     * @param id the id of the reserva to save.
     * @param reserva the reserva to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated reserva,
     * or with status {@code 400 (Bad Request)} if the reserva is not valid,
     * or with status {@code 404 (Not Found)} if the reserva is not found,
     * or with status {@code 500 (Internal Server Error)} if the reserva couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/reservas/{id}", consumes = "application/merge-patch+json")
    public Mono<ResponseEntity<Reserva>> partialUpdateReserva(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Reserva reserva
    ) throws URISyntaxException {
        log.debug("REST request to partial update Reserva partially : {}, {}", id, reserva);
        if (reserva.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, reserva.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return reservaRepository
            .existsById(id)
            .flatMap(
                exists -> {
                    if (!exists) {
                        return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                    }

                    Mono<Reserva> result = reservaRepository
                        .findById(reserva.getId())
                        .map(
                            existingReserva -> {
                                if (reserva.getFecha() != null) {
                                    existingReserva.setFecha(reserva.getFecha());
                                }
                                if (reserva.getHora() != null) {
                                    existingReserva.setHora(reserva.getHora());
                                }
                                if (reserva.getEspecialidad() != null) {
                                    existingReserva.setEspecialidad(reserva.getEspecialidad());
                                }

                                return existingReserva;
                            }
                        )
                        .flatMap(reservaRepository::save)
                        .flatMap(
                            savedReserva -> {
                                reservaSearchRepository.save(savedReserva);

                                return Mono.just(savedReserva);
                            }
                        );

                    return result
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                        .map(
                            res ->
                                ResponseEntity
                                    .ok()
                                    .headers(
                                        HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, res.getId().toString())
                                    )
                                    .body(res)
                        );
                }
            );
    }

    /**
     * {@code GET  /reservas} : get all the reservas.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of reservas in body.
     */
    @GetMapping("/reservas")
    public Mono<List<Reserva>> getAllReservas() {
        log.debug("REST request to get all Reservas");
        return reservaRepository.findAll().collectList();
    }

    /**
     * {@code GET  /reservas} : get all the reservas as a stream.
     * @return the {@link Flux} of reservas.
     */
    @GetMapping(value = "/reservas", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Reserva> getAllReservasAsStream() {
        log.debug("REST request to get all Reservas as a stream");
        return reservaRepository.findAll();
    }

    /**
     * {@code GET  /reservas/:id} : get the "id" reserva.
     *
     * @param id the id of the reserva to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the reserva, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/reservas/{id}")
    public Mono<ResponseEntity<Reserva>> getReserva(@PathVariable Long id) {
        log.debug("REST request to get Reserva : {}", id);
        Mono<Reserva> reserva = reservaRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(reserva);
    }

    /**
     * {@code DELETE  /reservas/:id} : delete the "id" reserva.
     *
     * @param id the id of the reserva to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/reservas/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> deleteReserva(@PathVariable Long id) {
        log.debug("REST request to delete Reserva : {}", id);
        return reservaRepository
            .deleteById(id)
            .then(reservaSearchRepository.deleteById(id))
            .map(
                result ->
                    ResponseEntity
                        .noContent()
                        .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
                        .build()
            );
    }

    /**
     * {@code SEARCH  /_search/reservas?query=:query} : search for the reserva corresponding
     * to the query.
     *
     * @param query the query of the reserva search.
     * @return the result of the search.
     */
    @GetMapping("/_search/reservas")
    public Mono<List<Reserva>> searchReservas(@RequestParam String query) {
        log.debug("REST request to search Reservas for query {}", query);
        return reservaSearchRepository.search(query).collectList();
    }
}
