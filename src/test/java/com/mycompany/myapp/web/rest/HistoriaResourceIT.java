package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Historia;
import com.mycompany.myapp.repository.HistoriaRepository;
import com.mycompany.myapp.repository.search.HistoriaSearchRepository;
import com.mycompany.myapp.service.EntityManager;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Integration tests for the {@link HistoriaResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient
@WithMockUser
class HistoriaResourceIT {

    private static final LocalDate DEFAULT_FECHA = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_FECHA = LocalDate.now(ZoneId.systemDefault());

    private static final String DEFAULT_DIAGNOSTICO = "AAAAAAAAAA";
    private static final String UPDATED_DIAGNOSTICO = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPCION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPCION = "BBBBBBBBBB";

    private static final String DEFAULT_RESULTADO_FILE = "AAAAAAAAAA";
    private static final String UPDATED_RESULTADO_FILE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/historias";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/_search/historias";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private HistoriaRepository historiaRepository;

    /**
     * This repository is mocked in the com.mycompany.myapp.repository.search test package.
     *
     * @see com.mycompany.myapp.repository.search.HistoriaSearchRepositoryMockConfiguration
     */
    @Autowired
    private HistoriaSearchRepository mockHistoriaSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Historia historia;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Historia createEntity(EntityManager em) {
        Historia historia = new Historia()
            .fecha(DEFAULT_FECHA)
            .diagnostico(DEFAULT_DIAGNOSTICO)
            .descripcion(DEFAULT_DESCRIPCION)
            .resultadoFile(DEFAULT_RESULTADO_FILE);
        return historia;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Historia createUpdatedEntity(EntityManager em) {
        Historia historia = new Historia()
            .fecha(UPDATED_FECHA)
            .diagnostico(UPDATED_DIAGNOSTICO)
            .descripcion(UPDATED_DESCRIPCION)
            .resultadoFile(UPDATED_RESULTADO_FILE);
        return historia;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Historia.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @AfterEach
    public void cleanup() {
        deleteEntities(em);
    }

    @BeforeEach
    public void initTest() {
        deleteEntities(em);
        historia = createEntity(em);
    }

    @Test
    void createHistoria() throws Exception {
        int databaseSizeBeforeCreate = historiaRepository.findAll().collectList().block().size();
        // Configure the mock search repository
        when(mockHistoriaSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Create the Historia
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(historia))
            .exchange()
            .expectStatus()
            .isCreated();

        // Validate the Historia in the database
        List<Historia> historiaList = historiaRepository.findAll().collectList().block();
        assertThat(historiaList).hasSize(databaseSizeBeforeCreate + 1);
        Historia testHistoria = historiaList.get(historiaList.size() - 1);
        assertThat(testHistoria.getFecha()).isEqualTo(DEFAULT_FECHA);
        assertThat(testHistoria.getDiagnostico()).isEqualTo(DEFAULT_DIAGNOSTICO);
        assertThat(testHistoria.getDescripcion()).isEqualTo(DEFAULT_DESCRIPCION);
        assertThat(testHistoria.getResultadoFile()).isEqualTo(DEFAULT_RESULTADO_FILE);

        // Validate the Historia in Elasticsearch
        verify(mockHistoriaSearchRepository, times(1)).save(testHistoria);
    }

    @Test
    void createHistoriaWithExistingId() throws Exception {
        // Create the Historia with an existing ID
        historia.setId(1L);

        int databaseSizeBeforeCreate = historiaRepository.findAll().collectList().block().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(historia))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Historia in the database
        List<Historia> historiaList = historiaRepository.findAll().collectList().block();
        assertThat(historiaList).hasSize(databaseSizeBeforeCreate);

        // Validate the Historia in Elasticsearch
        verify(mockHistoriaSearchRepository, times(0)).save(historia);
    }

    @Test
    void getAllHistoriasAsStream() {
        // Initialize the database
        historiaRepository.save(historia).block();

        List<Historia> historiaList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(Historia.class)
            .getResponseBody()
            .filter(historia::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(historiaList).isNotNull();
        assertThat(historiaList).hasSize(1);
        Historia testHistoria = historiaList.get(0);
        assertThat(testHistoria.getFecha()).isEqualTo(DEFAULT_FECHA);
        assertThat(testHistoria.getDiagnostico()).isEqualTo(DEFAULT_DIAGNOSTICO);
        assertThat(testHistoria.getDescripcion()).isEqualTo(DEFAULT_DESCRIPCION);
        assertThat(testHistoria.getResultadoFile()).isEqualTo(DEFAULT_RESULTADO_FILE);
    }

    @Test
    void getAllHistorias() {
        // Initialize the database
        historiaRepository.save(historia).block();

        // Get all the historiaList
        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(historia.getId().intValue()))
            .jsonPath("$.[*].fecha")
            .value(hasItem(DEFAULT_FECHA.toString()))
            .jsonPath("$.[*].diagnostico")
            .value(hasItem(DEFAULT_DIAGNOSTICO))
            .jsonPath("$.[*].descripcion")
            .value(hasItem(DEFAULT_DESCRIPCION))
            .jsonPath("$.[*].resultadoFile")
            .value(hasItem(DEFAULT_RESULTADO_FILE.toString()));
    }

    @Test
    void getHistoria() {
        // Initialize the database
        historiaRepository.save(historia).block();

        // Get the historia
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, historia.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(historia.getId().intValue()))
            .jsonPath("$.fecha")
            .value(is(DEFAULT_FECHA.toString()))
            .jsonPath("$.diagnostico")
            .value(is(DEFAULT_DIAGNOSTICO))
            .jsonPath("$.descripcion")
            .value(is(DEFAULT_DESCRIPCION))
            .jsonPath("$.resultadoFile")
            .value(is(DEFAULT_RESULTADO_FILE.toString()));
    }

    @Test
    void getNonExistingHistoria() {
        // Get the historia
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putNewHistoria() throws Exception {
        // Configure the mock search repository
        when(mockHistoriaSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Initialize the database
        historiaRepository.save(historia).block();

        int databaseSizeBeforeUpdate = historiaRepository.findAll().collectList().block().size();

        // Update the historia
        Historia updatedHistoria = historiaRepository.findById(historia.getId()).block();
        updatedHistoria
            .fecha(UPDATED_FECHA)
            .diagnostico(UPDATED_DIAGNOSTICO)
            .descripcion(UPDATED_DESCRIPCION)
            .resultadoFile(UPDATED_RESULTADO_FILE);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedHistoria.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(updatedHistoria))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Historia in the database
        List<Historia> historiaList = historiaRepository.findAll().collectList().block();
        assertThat(historiaList).hasSize(databaseSizeBeforeUpdate);
        Historia testHistoria = historiaList.get(historiaList.size() - 1);
        assertThat(testHistoria.getFecha()).isEqualTo(UPDATED_FECHA);
        assertThat(testHistoria.getDiagnostico()).isEqualTo(UPDATED_DIAGNOSTICO);
        assertThat(testHistoria.getDescripcion()).isEqualTo(UPDATED_DESCRIPCION);
        assertThat(testHistoria.getResultadoFile()).isEqualTo(UPDATED_RESULTADO_FILE);

        // Validate the Historia in Elasticsearch
        verify(mockHistoriaSearchRepository).save(testHistoria);
    }

    @Test
    void putNonExistingHistoria() throws Exception {
        int databaseSizeBeforeUpdate = historiaRepository.findAll().collectList().block().size();
        historia.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, historia.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(historia))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Historia in the database
        List<Historia> historiaList = historiaRepository.findAll().collectList().block();
        assertThat(historiaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Historia in Elasticsearch
        verify(mockHistoriaSearchRepository, times(0)).save(historia);
    }

    @Test
    void putWithIdMismatchHistoria() throws Exception {
        int databaseSizeBeforeUpdate = historiaRepository.findAll().collectList().block().size();
        historia.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(historia))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Historia in the database
        List<Historia> historiaList = historiaRepository.findAll().collectList().block();
        assertThat(historiaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Historia in Elasticsearch
        verify(mockHistoriaSearchRepository, times(0)).save(historia);
    }

    @Test
    void putWithMissingIdPathParamHistoria() throws Exception {
        int databaseSizeBeforeUpdate = historiaRepository.findAll().collectList().block().size();
        historia.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(historia))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Historia in the database
        List<Historia> historiaList = historiaRepository.findAll().collectList().block();
        assertThat(historiaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Historia in Elasticsearch
        verify(mockHistoriaSearchRepository, times(0)).save(historia);
    }

    @Test
    void partialUpdateHistoriaWithPatch() throws Exception {
        // Initialize the database
        historiaRepository.save(historia).block();

        int databaseSizeBeforeUpdate = historiaRepository.findAll().collectList().block().size();

        // Update the historia using partial update
        Historia partialUpdatedHistoria = new Historia();
        partialUpdatedHistoria.setId(historia.getId());

        partialUpdatedHistoria.fecha(UPDATED_FECHA).diagnostico(UPDATED_DIAGNOSTICO).resultadoFile(UPDATED_RESULTADO_FILE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedHistoria.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedHistoria))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Historia in the database
        List<Historia> historiaList = historiaRepository.findAll().collectList().block();
        assertThat(historiaList).hasSize(databaseSizeBeforeUpdate);
        Historia testHistoria = historiaList.get(historiaList.size() - 1);
        assertThat(testHistoria.getFecha()).isEqualTo(UPDATED_FECHA);
        assertThat(testHistoria.getDiagnostico()).isEqualTo(UPDATED_DIAGNOSTICO);
        assertThat(testHistoria.getDescripcion()).isEqualTo(DEFAULT_DESCRIPCION);
        assertThat(testHistoria.getResultadoFile()).isEqualTo(UPDATED_RESULTADO_FILE);
    }

    @Test
    void fullUpdateHistoriaWithPatch() throws Exception {
        // Initialize the database
        historiaRepository.save(historia).block();

        int databaseSizeBeforeUpdate = historiaRepository.findAll().collectList().block().size();

        // Update the historia using partial update
        Historia partialUpdatedHistoria = new Historia();
        partialUpdatedHistoria.setId(historia.getId());

        partialUpdatedHistoria
            .fecha(UPDATED_FECHA)
            .diagnostico(UPDATED_DIAGNOSTICO)
            .descripcion(UPDATED_DESCRIPCION)
            .resultadoFile(UPDATED_RESULTADO_FILE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedHistoria.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedHistoria))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Historia in the database
        List<Historia> historiaList = historiaRepository.findAll().collectList().block();
        assertThat(historiaList).hasSize(databaseSizeBeforeUpdate);
        Historia testHistoria = historiaList.get(historiaList.size() - 1);
        assertThat(testHistoria.getFecha()).isEqualTo(UPDATED_FECHA);
        assertThat(testHistoria.getDiagnostico()).isEqualTo(UPDATED_DIAGNOSTICO);
        assertThat(testHistoria.getDescripcion()).isEqualTo(UPDATED_DESCRIPCION);
        assertThat(testHistoria.getResultadoFile()).isEqualTo(UPDATED_RESULTADO_FILE);
    }

    @Test
    void patchNonExistingHistoria() throws Exception {
        int databaseSizeBeforeUpdate = historiaRepository.findAll().collectList().block().size();
        historia.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, historia.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(historia))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Historia in the database
        List<Historia> historiaList = historiaRepository.findAll().collectList().block();
        assertThat(historiaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Historia in Elasticsearch
        verify(mockHistoriaSearchRepository, times(0)).save(historia);
    }

    @Test
    void patchWithIdMismatchHistoria() throws Exception {
        int databaseSizeBeforeUpdate = historiaRepository.findAll().collectList().block().size();
        historia.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(historia))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Historia in the database
        List<Historia> historiaList = historiaRepository.findAll().collectList().block();
        assertThat(historiaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Historia in Elasticsearch
        verify(mockHistoriaSearchRepository, times(0)).save(historia);
    }

    @Test
    void patchWithMissingIdPathParamHistoria() throws Exception {
        int databaseSizeBeforeUpdate = historiaRepository.findAll().collectList().block().size();
        historia.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(historia))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Historia in the database
        List<Historia> historiaList = historiaRepository.findAll().collectList().block();
        assertThat(historiaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Historia in Elasticsearch
        verify(mockHistoriaSearchRepository, times(0)).save(historia);
    }

    @Test
    void deleteHistoria() {
        // Configure the mock search repository
        when(mockHistoriaSearchRepository.deleteById(anyLong())).thenReturn(Mono.empty());
        // Initialize the database
        historiaRepository.save(historia).block();

        int databaseSizeBeforeDelete = historiaRepository.findAll().collectList().block().size();

        // Delete the historia
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, historia.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        List<Historia> historiaList = historiaRepository.findAll().collectList().block();
        assertThat(historiaList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the Historia in Elasticsearch
        verify(mockHistoriaSearchRepository, times(1)).deleteById(historia.getId());
    }

    @Test
    void searchHistoria() {
        // Configure the mock search repository
        when(mockHistoriaSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Initialize the database
        historiaRepository.save(historia).block();
        when(mockHistoriaSearchRepository.search("id:" + historia.getId())).thenReturn(Flux.just(historia));

        // Search the historia
        webTestClient
            .get()
            .uri(ENTITY_SEARCH_API_URL + "?query=id:" + historia.getId())
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(historia.getId().intValue()))
            .jsonPath("$.[*].fecha")
            .value(hasItem(DEFAULT_FECHA.toString()))
            .jsonPath("$.[*].diagnostico")
            .value(hasItem(DEFAULT_DIAGNOSTICO))
            .jsonPath("$.[*].descripcion")
            .value(hasItem(DEFAULT_DESCRIPCION))
            .jsonPath("$.[*].resultadoFile")
            .value(hasItem(DEFAULT_RESULTADO_FILE.toString()));
    }
}
