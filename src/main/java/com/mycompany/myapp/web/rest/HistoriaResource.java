package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.Historia;
import com.mycompany.myapp.repository.HistoriaRepository;
import com.mycompany.myapp.repository.search.HistoriaSearchRepository;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.Historia}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class HistoriaResource {

    private final Logger log = LoggerFactory.getLogger(HistoriaResource.class);

    private static final String ENTITY_NAME = "mavenHistoria";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final HistoriaRepository historiaRepository;

    private final HistoriaSearchRepository historiaSearchRepository;

    public HistoriaResource(HistoriaRepository historiaRepository, HistoriaSearchRepository historiaSearchRepository) {
        this.historiaRepository = historiaRepository;
        this.historiaSearchRepository = historiaSearchRepository;
    }

    /**
     * {@code POST  /historias} : Create a new historia.
     *
     * @param historia the historia to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new historia, or with status {@code 400 (Bad Request)} if the historia has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/historias")
    public Mono<ResponseEntity<Historia>> createHistoria(@RequestBody Historia historia) throws URISyntaxException {
        log.debug("REST request to save Historia : {}", historia);
        if (historia.getId() != null) {
            throw new BadRequestAlertException("A new historia cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return historiaRepository
            .save(historia)
            .flatMap(historiaSearchRepository::save)
            .map(
                result -> {
                    try {
                        return ResponseEntity
                            .created(new URI("/api/historias/" + result.getId()))
                            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
    }

    /**
     * {@code PUT  /historias/:id} : Updates an existing historia.
     *
     * @param id the id of the historia to save.
     * @param historia the historia to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated historia,
     * or with status {@code 400 (Bad Request)} if the historia is not valid,
     * or with status {@code 500 (Internal Server Error)} if the historia couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/historias/{id}")
    public Mono<ResponseEntity<Historia>> updateHistoria(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Historia historia
    ) throws URISyntaxException {
        log.debug("REST request to update Historia : {}, {}", id, historia);
        if (historia.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, historia.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return historiaRepository
            .existsById(id)
            .flatMap(
                exists -> {
                    if (!exists) {
                        return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                    }

                    return historiaRepository
                        .save(historia)
                        .flatMap(historiaSearchRepository::save)
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
     * {@code PATCH  /historias/:id} : Partial updates given fields of an existing historia, field will ignore if it is null
     *
     * @param id the id of the historia to save.
     * @param historia the historia to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated historia,
     * or with status {@code 400 (Bad Request)} if the historia is not valid,
     * or with status {@code 404 (Not Found)} if the historia is not found,
     * or with status {@code 500 (Internal Server Error)} if the historia couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/historias/{id}", consumes = "application/merge-patch+json")
    public Mono<ResponseEntity<Historia>> partialUpdateHistoria(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Historia historia
    ) throws URISyntaxException {
        log.debug("REST request to partial update Historia partially : {}, {}", id, historia);
        if (historia.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, historia.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return historiaRepository
            .existsById(id)
            .flatMap(
                exists -> {
                    if (!exists) {
                        return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                    }

                    Mono<Historia> result = historiaRepository
                        .findById(historia.getId())
                        .map(
                            existingHistoria -> {
                                if (historia.getFecha() != null) {
                                    existingHistoria.setFecha(historia.getFecha());
                                }
                                if (historia.getDiagnostico() != null) {
                                    existingHistoria.setDiagnostico(historia.getDiagnostico());
                                }
                                if (historia.getDescripcion() != null) {
                                    existingHistoria.setDescripcion(historia.getDescripcion());
                                }
                                if (historia.getResultadoFile() != null) {
                                    existingHistoria.setResultadoFile(historia.getResultadoFile());
                                }

                                return existingHistoria;
                            }
                        )
                        .flatMap(historiaRepository::save)
                        .flatMap(
                            savedHistoria -> {
                                historiaSearchRepository.save(savedHistoria);

                                return Mono.just(savedHistoria);
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
     * {@code GET  /historias} : get all the historias.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of historias in body.
     */
    @GetMapping("/historias")
    public Mono<List<Historia>> getAllHistorias() {
        log.debug("REST request to get all Historias");
        return historiaRepository.findAll().collectList();
    }

    /**
     * {@code GET  /historias} : get all the historias as a stream.
     * @return the {@link Flux} of historias.
     */
    @GetMapping(value = "/historias", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Historia> getAllHistoriasAsStream() {
        log.debug("REST request to get all Historias as a stream");
        return historiaRepository.findAll();
    }

    /**
     * {@code GET  /historias/:id} : get the "id" historia.
     *
     * @param id the id of the historia to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the historia, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/historias/{id}")
    public Mono<ResponseEntity<Historia>> getHistoria(@PathVariable Long id) {
        log.debug("REST request to get Historia : {}", id);
        Mono<Historia> historia = historiaRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(historia);
    }

    /**
     * {@code DELETE  /historias/:id} : delete the "id" historia.
     *
     * @param id the id of the historia to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/historias/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> deleteHistoria(@PathVariable Long id) {
        log.debug("REST request to delete Historia : {}", id);
        return historiaRepository
            .deleteById(id)
            .then(historiaSearchRepository.deleteById(id))
            .map(
                result ->
                    ResponseEntity
                        .noContent()
                        .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
                        .build()
            );
    }

    /**
     * {@code SEARCH  /_search/historias?query=:query} : search for the historia corresponding
     * to the query.
     *
     * @param query the query of the historia search.
     * @return the result of the search.
     */
    @GetMapping("/_search/historias")
    public Mono<List<Historia>> searchHistorias(@RequestParam String query) {
        log.debug("REST request to search Historias for query {}", query);
        return historiaSearchRepository.search(query).collectList();
    }
}
