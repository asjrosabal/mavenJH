package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.Especialista;
import com.mycompany.myapp.repository.EspecialistaRepository;
import com.mycompany.myapp.repository.search.EspecialistaSearchRepository;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.Especialista}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class EspecialistaResource {

    private final Logger log = LoggerFactory.getLogger(EspecialistaResource.class);

    private static final String ENTITY_NAME = "mavenEspecialista";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final EspecialistaRepository especialistaRepository;

    private final EspecialistaSearchRepository especialistaSearchRepository;

    public EspecialistaResource(EspecialistaRepository especialistaRepository, EspecialistaSearchRepository especialistaSearchRepository) {
        this.especialistaRepository = especialistaRepository;
        this.especialistaSearchRepository = especialistaSearchRepository;
    }

    /**
     * {@code POST  /especialistas} : Create a new especialista.
     *
     * @param especialista the especialista to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new especialista, or with status {@code 400 (Bad Request)} if the especialista has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/especialistas")
    public Mono<ResponseEntity<Especialista>> createEspecialista(@RequestBody Especialista especialista) throws URISyntaxException {
        log.debug("REST request to save Especialista : {}", especialista);
        if (especialista.getId() != null) {
            throw new BadRequestAlertException("A new especialista cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return especialistaRepository
            .save(especialista)
            .flatMap(especialistaSearchRepository::save)
            .map(
                result -> {
                    try {
                        return ResponseEntity
                            .created(new URI("/api/especialistas/" + result.getId()))
                            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
    }

    /**
     * {@code PUT  /especialistas/:id} : Updates an existing especialista.
     *
     * @param id the id of the especialista to save.
     * @param especialista the especialista to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated especialista,
     * or with status {@code 400 (Bad Request)} if the especialista is not valid,
     * or with status {@code 500 (Internal Server Error)} if the especialista couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/especialistas/{id}")
    public Mono<ResponseEntity<Especialista>> updateEspecialista(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Especialista especialista
    ) throws URISyntaxException {
        log.debug("REST request to update Especialista : {}, {}", id, especialista);
        if (especialista.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, especialista.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return especialistaRepository
            .existsById(id)
            .flatMap(
                exists -> {
                    if (!exists) {
                        return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                    }

                    return especialistaRepository
                        .save(especialista)
                        .flatMap(especialistaSearchRepository::save)
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
     * {@code PATCH  /especialistas/:id} : Partial updates given fields of an existing especialista, field will ignore if it is null
     *
     * @param id the id of the especialista to save.
     * @param especialista the especialista to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated especialista,
     * or with status {@code 400 (Bad Request)} if the especialista is not valid,
     * or with status {@code 404 (Not Found)} if the especialista is not found,
     * or with status {@code 500 (Internal Server Error)} if the especialista couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/especialistas/{id}", consumes = "application/merge-patch+json")
    public Mono<ResponseEntity<Especialista>> partialUpdateEspecialista(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Especialista especialista
    ) throws URISyntaxException {
        log.debug("REST request to partial update Especialista partially : {}, {}", id, especialista);
        if (especialista.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, especialista.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return especialistaRepository
            .existsById(id)
            .flatMap(
                exists -> {
                    if (!exists) {
                        return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                    }

                    Mono<Especialista> result = especialistaRepository
                        .findById(especialista.getId())
                        .map(
                            existingEspecialista -> {
                                if (especialista.getNombre() != null) {
                                    existingEspecialista.setNombre(especialista.getNombre());
                                }
                                if (especialista.getApellidos() != null) {
                                    existingEspecialista.setApellidos(especialista.getApellidos());
                                }
                                if (especialista.getRut() != null) {
                                    existingEspecialista.setRut(especialista.getRut());
                                }
                                if (especialista.getFechaNacimiento() != null) {
                                    existingEspecialista.setFechaNacimiento(especialista.getFechaNacimiento());
                                }
                                if (especialista.getRegistroMedico() != null) {
                                    existingEspecialista.setRegistroMedico(especialista.getRegistroMedico());
                                }
                                if (especialista.getEspecialidad() != null) {
                                    existingEspecialista.setEspecialidad(especialista.getEspecialidad());
                                }

                                return existingEspecialista;
                            }
                        )
                        .flatMap(especialistaRepository::save)
                        .flatMap(
                            savedEspecialista -> {
                                especialistaSearchRepository.save(savedEspecialista);

                                return Mono.just(savedEspecialista);
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
     * {@code GET  /especialistas} : get all the especialistas.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of especialistas in body.
     */
    @GetMapping("/especialistas")
    public Mono<List<Especialista>> getAllEspecialistas() {
        log.debug("REST request to get all Especialistas");
        return especialistaRepository.findAll().collectList();
    }

    /**
     * {@code GET  /especialistas} : get all the especialistas as a stream.
     * @return the {@link Flux} of especialistas.
     */
    @GetMapping(value = "/especialistas", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Especialista> getAllEspecialistasAsStream() {
        log.debug("REST request to get all Especialistas as a stream");
        return especialistaRepository.findAll();
    }

    /**
     * {@code GET  /especialistas/:id} : get the "id" especialista.
     *
     * @param id the id of the especialista to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the especialista, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/especialistas/{id}")
    public Mono<ResponseEntity<Especialista>> getEspecialista(@PathVariable Long id) {
        log.debug("REST request to get Especialista : {}", id);
        Mono<Especialista> especialista = especialistaRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(especialista);
    }

    /**
     * {@code DELETE  /especialistas/:id} : delete the "id" especialista.
     *
     * @param id the id of the especialista to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/especialistas/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> deleteEspecialista(@PathVariable Long id) {
        log.debug("REST request to delete Especialista : {}", id);
        return especialistaRepository
            .deleteById(id)
            .then(especialistaSearchRepository.deleteById(id))
            .map(
                result ->
                    ResponseEntity
                        .noContent()
                        .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
                        .build()
            );
    }

    /**
     * {@code SEARCH  /_search/especialistas?query=:query} : search for the especialista corresponding
     * to the query.
     *
     * @param query the query of the especialista search.
     * @return the result of the search.
     */
    @GetMapping("/_search/especialistas")
    public Mono<List<Especialista>> searchEspecialistas(@RequestParam String query) {
        log.debug("REST request to search Especialistas for query {}", query);
        return especialistaSearchRepository.search(query).collectList();
    }
}
