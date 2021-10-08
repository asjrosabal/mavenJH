package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.Paciente;
import com.mycompany.myapp.repository.PacienteRepository;
import com.mycompany.myapp.repository.search.PacienteSearchRepository;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.Paciente}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class PacienteResource {

    private final Logger log = LoggerFactory.getLogger(PacienteResource.class);

    private static final String ENTITY_NAME = "mavenPaciente";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PacienteRepository pacienteRepository;

    private final PacienteSearchRepository pacienteSearchRepository;

    public PacienteResource(PacienteRepository pacienteRepository, PacienteSearchRepository pacienteSearchRepository) {
        this.pacienteRepository = pacienteRepository;
        this.pacienteSearchRepository = pacienteSearchRepository;
    }

    /**
     * {@code POST  /pacientes} : Create a new paciente.
     *
     * @param paciente the paciente to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new paciente, or with status {@code 400 (Bad Request)} if the paciente has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/pacientes")
    public Mono<ResponseEntity<Paciente>> createPaciente(@RequestBody Paciente paciente) throws URISyntaxException {
        log.debug("REST request to save Paciente : {}", paciente);
        if (paciente.getId() != null) {
            throw new BadRequestAlertException("A new paciente cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return pacienteRepository
            .save(paciente)
            .flatMap(pacienteSearchRepository::save)
            .map(
                result -> {
                    try {
                        return ResponseEntity
                            .created(new URI("/api/pacientes/" + result.getId()))
                            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
    }

    /**
     * {@code PUT  /pacientes/:id} : Updates an existing paciente.
     *
     * @param id the id of the paciente to save.
     * @param paciente the paciente to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated paciente,
     * or with status {@code 400 (Bad Request)} if the paciente is not valid,
     * or with status {@code 500 (Internal Server Error)} if the paciente couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/pacientes/{id}")
    public Mono<ResponseEntity<Paciente>> updatePaciente(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Paciente paciente
    ) throws URISyntaxException {
        log.debug("REST request to update Paciente : {}, {}", id, paciente);
        if (paciente.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, paciente.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return pacienteRepository
            .existsById(id)
            .flatMap(
                exists -> {
                    if (!exists) {
                        return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                    }

                    return pacienteRepository
                        .save(paciente)
                        .flatMap(pacienteSearchRepository::save)
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
     * {@code PATCH  /pacientes/:id} : Partial updates given fields of an existing paciente, field will ignore if it is null
     *
     * @param id the id of the paciente to save.
     * @param paciente the paciente to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated paciente,
     * or with status {@code 400 (Bad Request)} if the paciente is not valid,
     * or with status {@code 404 (Not Found)} if the paciente is not found,
     * or with status {@code 500 (Internal Server Error)} if the paciente couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/pacientes/{id}", consumes = "application/merge-patch+json")
    public Mono<ResponseEntity<Paciente>> partialUpdatePaciente(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Paciente paciente
    ) throws URISyntaxException {
        log.debug("REST request to partial update Paciente partially : {}, {}", id, paciente);
        if (paciente.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, paciente.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return pacienteRepository
            .existsById(id)
            .flatMap(
                exists -> {
                    if (!exists) {
                        return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                    }

                    Mono<Paciente> result = pacienteRepository
                        .findById(paciente.getId())
                        .map(
                            existingPaciente -> {
                                if (paciente.getNombre() != null) {
                                    existingPaciente.setNombre(paciente.getNombre());
                                }
                                if (paciente.getApellidos() != null) {
                                    existingPaciente.setApellidos(paciente.getApellidos());
                                }
                                if (paciente.getRut() != null) {
                                    existingPaciente.setRut(paciente.getRut());
                                }
                                if (paciente.getFechaNacimiento() != null) {
                                    existingPaciente.setFechaNacimiento(paciente.getFechaNacimiento());
                                }

                                return existingPaciente;
                            }
                        )
                        .flatMap(pacienteRepository::save)
                        .flatMap(
                            savedPaciente -> {
                                pacienteSearchRepository.save(savedPaciente);

                                return Mono.just(savedPaciente);
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
     * {@code GET  /pacientes} : get all the pacientes.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of pacientes in body.
     */
    @GetMapping("/pacientes")
    public Mono<List<Paciente>> getAllPacientes() {
        log.debug("REST request to get all Pacientes");
        return pacienteRepository.findAll().collectList();
    }

    /**
     * {@code GET  /pacientes} : get all the pacientes as a stream.
     * @return the {@link Flux} of pacientes.
     */
    @GetMapping(value = "/pacientes", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Paciente> getAllPacientesAsStream() {
        log.debug("REST request to get all Pacientes as a stream");
        return pacienteRepository.findAll();
    }

    /**
     * {@code GET  /pacientes/:id} : get the "id" paciente.
     *
     * @param id the id of the paciente to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the paciente, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/pacientes/{id}")
    public Mono<ResponseEntity<Paciente>> getPaciente(@PathVariable Long id) {
        log.debug("REST request to get Paciente : {}", id);
        Mono<Paciente> paciente = pacienteRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(paciente);
    }

    /**
     * {@code DELETE  /pacientes/:id} : delete the "id" paciente.
     *
     * @param id the id of the paciente to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/pacientes/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> deletePaciente(@PathVariable Long id) {
        log.debug("REST request to delete Paciente : {}", id);
        return pacienteRepository
            .deleteById(id)
            .then(pacienteSearchRepository.deleteById(id))
            .map(
                result ->
                    ResponseEntity
                        .noContent()
                        .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
                        .build()
            );
    }

    /**
     * {@code SEARCH  /_search/pacientes?query=:query} : search for the paciente corresponding
     * to the query.
     *
     * @param query the query of the paciente search.
     * @return the result of the search.
     */
    @GetMapping("/_search/pacientes")
    public Mono<List<Paciente>> searchPacientes(@RequestParam String query) {
        log.debug("REST request to search Pacientes for query {}", query);
        return pacienteSearchRepository.search(query).collectList();
    }
}
