package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Reserva;
import com.mycompany.myapp.domain.enumeration.Especialidad;
import com.mycompany.myapp.repository.ReservaRepository;
import com.mycompany.myapp.repository.search.ReservaSearchRepository;
import com.mycompany.myapp.service.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Integration tests for the {@link ReservaResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient
@WithMockUser
class ReservaResourceIT {

    private static final LocalDate DEFAULT_FECHA = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_FECHA = LocalDate.now(ZoneId.systemDefault());

    private static final Instant DEFAULT_HORA = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_HORA = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Especialidad DEFAULT_ESPECIALIDAD = Especialidad.MEDICINA_GENERAL;
    private static final Especialidad UPDATED_ESPECIALIDAD = Especialidad.LABORATORIO;

    private static final String ENTITY_API_URL = "/api/reservas";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/_search/reservas";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ReservaRepository reservaRepository;

    /**
     * This repository is mocked in the com.mycompany.myapp.repository.search test package.
     *
     * @see com.mycompany.myapp.repository.search.ReservaSearchRepositoryMockConfiguration
     */
    @Autowired
    private ReservaSearchRepository mockReservaSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Reserva reserva;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Reserva createEntity(EntityManager em) {
        Reserva reserva = new Reserva().fecha(DEFAULT_FECHA).hora(DEFAULT_HORA).especialidad(DEFAULT_ESPECIALIDAD);
        return reserva;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Reserva createUpdatedEntity(EntityManager em) {
        Reserva reserva = new Reserva().fecha(UPDATED_FECHA).hora(UPDATED_HORA).especialidad(UPDATED_ESPECIALIDAD);
        return reserva;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Reserva.class).block();
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
        reserva = createEntity(em);
    }

    @Test
    void createReserva() throws Exception {
        int databaseSizeBeforeCreate = reservaRepository.findAll().collectList().block().size();
        // Configure the mock search repository
        when(mockReservaSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Create the Reserva
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(reserva))
            .exchange()
            .expectStatus()
            .isCreated();

        // Validate the Reserva in the database
        List<Reserva> reservaList = reservaRepository.findAll().collectList().block();
        assertThat(reservaList).hasSize(databaseSizeBeforeCreate + 1);
        Reserva testReserva = reservaList.get(reservaList.size() - 1);
        assertThat(testReserva.getFecha()).isEqualTo(DEFAULT_FECHA);
        assertThat(testReserva.getHora()).isEqualTo(DEFAULT_HORA);
        assertThat(testReserva.getEspecialidad()).isEqualTo(DEFAULT_ESPECIALIDAD);

        // Validate the Reserva in Elasticsearch
        verify(mockReservaSearchRepository, times(1)).save(testReserva);
    }

    @Test
    void createReservaWithExistingId() throws Exception {
        // Create the Reserva with an existing ID
        reserva.setId(1L);

        int databaseSizeBeforeCreate = reservaRepository.findAll().collectList().block().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(reserva))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Reserva in the database
        List<Reserva> reservaList = reservaRepository.findAll().collectList().block();
        assertThat(reservaList).hasSize(databaseSizeBeforeCreate);

        // Validate the Reserva in Elasticsearch
        verify(mockReservaSearchRepository, times(0)).save(reserva);
    }

    @Test
    void getAllReservasAsStream() {
        // Initialize the database
        reservaRepository.save(reserva).block();

        List<Reserva> reservaList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(Reserva.class)
            .getResponseBody()
            .filter(reserva::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(reservaList).isNotNull();
        assertThat(reservaList).hasSize(1);
        Reserva testReserva = reservaList.get(0);
        assertThat(testReserva.getFecha()).isEqualTo(DEFAULT_FECHA);
        assertThat(testReserva.getHora()).isEqualTo(DEFAULT_HORA);
        assertThat(testReserva.getEspecialidad()).isEqualTo(DEFAULT_ESPECIALIDAD);
    }

    @Test
    void getAllReservas() {
        // Initialize the database
        reservaRepository.save(reserva).block();

        // Get all the reservaList
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
            .value(hasItem(reserva.getId().intValue()))
            .jsonPath("$.[*].fecha")
            .value(hasItem(DEFAULT_FECHA.toString()))
            .jsonPath("$.[*].hora")
            .value(hasItem(DEFAULT_HORA.toString()))
            .jsonPath("$.[*].especialidad")
            .value(hasItem(DEFAULT_ESPECIALIDAD.toString()));
    }

    @Test
    void getReserva() {
        // Initialize the database
        reservaRepository.save(reserva).block();

        // Get the reserva
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, reserva.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(reserva.getId().intValue()))
            .jsonPath("$.fecha")
            .value(is(DEFAULT_FECHA.toString()))
            .jsonPath("$.hora")
            .value(is(DEFAULT_HORA.toString()))
            .jsonPath("$.especialidad")
            .value(is(DEFAULT_ESPECIALIDAD.toString()));
    }

    @Test
    void getNonExistingReserva() {
        // Get the reserva
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putNewReserva() throws Exception {
        // Configure the mock search repository
        when(mockReservaSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Initialize the database
        reservaRepository.save(reserva).block();

        int databaseSizeBeforeUpdate = reservaRepository.findAll().collectList().block().size();

        // Update the reserva
        Reserva updatedReserva = reservaRepository.findById(reserva.getId()).block();
        updatedReserva.fecha(UPDATED_FECHA).hora(UPDATED_HORA).especialidad(UPDATED_ESPECIALIDAD);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedReserva.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(updatedReserva))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Reserva in the database
        List<Reserva> reservaList = reservaRepository.findAll().collectList().block();
        assertThat(reservaList).hasSize(databaseSizeBeforeUpdate);
        Reserva testReserva = reservaList.get(reservaList.size() - 1);
        assertThat(testReserva.getFecha()).isEqualTo(UPDATED_FECHA);
        assertThat(testReserva.getHora()).isEqualTo(UPDATED_HORA);
        assertThat(testReserva.getEspecialidad()).isEqualTo(UPDATED_ESPECIALIDAD);

        // Validate the Reserva in Elasticsearch
        verify(mockReservaSearchRepository).save(testReserva);
    }

    @Test
    void putNonExistingReserva() throws Exception {
        int databaseSizeBeforeUpdate = reservaRepository.findAll().collectList().block().size();
        reserva.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, reserva.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(reserva))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Reserva in the database
        List<Reserva> reservaList = reservaRepository.findAll().collectList().block();
        assertThat(reservaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Reserva in Elasticsearch
        verify(mockReservaSearchRepository, times(0)).save(reserva);
    }

    @Test
    void putWithIdMismatchReserva() throws Exception {
        int databaseSizeBeforeUpdate = reservaRepository.findAll().collectList().block().size();
        reserva.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(reserva))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Reserva in the database
        List<Reserva> reservaList = reservaRepository.findAll().collectList().block();
        assertThat(reservaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Reserva in Elasticsearch
        verify(mockReservaSearchRepository, times(0)).save(reserva);
    }

    @Test
    void putWithMissingIdPathParamReserva() throws Exception {
        int databaseSizeBeforeUpdate = reservaRepository.findAll().collectList().block().size();
        reserva.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(reserva))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Reserva in the database
        List<Reserva> reservaList = reservaRepository.findAll().collectList().block();
        assertThat(reservaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Reserva in Elasticsearch
        verify(mockReservaSearchRepository, times(0)).save(reserva);
    }

    @Test
    void partialUpdateReservaWithPatch() throws Exception {
        // Initialize the database
        reservaRepository.save(reserva).block();

        int databaseSizeBeforeUpdate = reservaRepository.findAll().collectList().block().size();

        // Update the reserva using partial update
        Reserva partialUpdatedReserva = new Reserva();
        partialUpdatedReserva.setId(reserva.getId());

        partialUpdatedReserva.fecha(UPDATED_FECHA).hora(UPDATED_HORA).especialidad(UPDATED_ESPECIALIDAD);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedReserva.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedReserva))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Reserva in the database
        List<Reserva> reservaList = reservaRepository.findAll().collectList().block();
        assertThat(reservaList).hasSize(databaseSizeBeforeUpdate);
        Reserva testReserva = reservaList.get(reservaList.size() - 1);
        assertThat(testReserva.getFecha()).isEqualTo(UPDATED_FECHA);
        assertThat(testReserva.getHora()).isEqualTo(UPDATED_HORA);
        assertThat(testReserva.getEspecialidad()).isEqualTo(UPDATED_ESPECIALIDAD);
    }

    @Test
    void fullUpdateReservaWithPatch() throws Exception {
        // Initialize the database
        reservaRepository.save(reserva).block();

        int databaseSizeBeforeUpdate = reservaRepository.findAll().collectList().block().size();

        // Update the reserva using partial update
        Reserva partialUpdatedReserva = new Reserva();
        partialUpdatedReserva.setId(reserva.getId());

        partialUpdatedReserva.fecha(UPDATED_FECHA).hora(UPDATED_HORA).especialidad(UPDATED_ESPECIALIDAD);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedReserva.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedReserva))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Reserva in the database
        List<Reserva> reservaList = reservaRepository.findAll().collectList().block();
        assertThat(reservaList).hasSize(databaseSizeBeforeUpdate);
        Reserva testReserva = reservaList.get(reservaList.size() - 1);
        assertThat(testReserva.getFecha()).isEqualTo(UPDATED_FECHA);
        assertThat(testReserva.getHora()).isEqualTo(UPDATED_HORA);
        assertThat(testReserva.getEspecialidad()).isEqualTo(UPDATED_ESPECIALIDAD);
    }

    @Test
    void patchNonExistingReserva() throws Exception {
        int databaseSizeBeforeUpdate = reservaRepository.findAll().collectList().block().size();
        reserva.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, reserva.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(reserva))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Reserva in the database
        List<Reserva> reservaList = reservaRepository.findAll().collectList().block();
        assertThat(reservaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Reserva in Elasticsearch
        verify(mockReservaSearchRepository, times(0)).save(reserva);
    }

    @Test
    void patchWithIdMismatchReserva() throws Exception {
        int databaseSizeBeforeUpdate = reservaRepository.findAll().collectList().block().size();
        reserva.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(reserva))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Reserva in the database
        List<Reserva> reservaList = reservaRepository.findAll().collectList().block();
        assertThat(reservaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Reserva in Elasticsearch
        verify(mockReservaSearchRepository, times(0)).save(reserva);
    }

    @Test
    void patchWithMissingIdPathParamReserva() throws Exception {
        int databaseSizeBeforeUpdate = reservaRepository.findAll().collectList().block().size();
        reserva.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(reserva))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Reserva in the database
        List<Reserva> reservaList = reservaRepository.findAll().collectList().block();
        assertThat(reservaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Reserva in Elasticsearch
        verify(mockReservaSearchRepository, times(0)).save(reserva);
    }

    @Test
    void deleteReserva() {
        // Configure the mock search repository
        when(mockReservaSearchRepository.deleteById(anyLong())).thenReturn(Mono.empty());
        // Initialize the database
        reservaRepository.save(reserva).block();

        int databaseSizeBeforeDelete = reservaRepository.findAll().collectList().block().size();

        // Delete the reserva
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, reserva.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        List<Reserva> reservaList = reservaRepository.findAll().collectList().block();
        assertThat(reservaList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the Reserva in Elasticsearch
        verify(mockReservaSearchRepository, times(1)).deleteById(reserva.getId());
    }

    @Test
    void searchReserva() {
        // Configure the mock search repository
        when(mockReservaSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Initialize the database
        reservaRepository.save(reserva).block();
        when(mockReservaSearchRepository.search("id:" + reserva.getId())).thenReturn(Flux.just(reserva));

        // Search the reserva
        webTestClient
            .get()
            .uri(ENTITY_SEARCH_API_URL + "?query=id:" + reserva.getId())
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(reserva.getId().intValue()))
            .jsonPath("$.[*].fecha")
            .value(hasItem(DEFAULT_FECHA.toString()))
            .jsonPath("$.[*].hora")
            .value(hasItem(DEFAULT_HORA.toString()))
            .jsonPath("$.[*].especialidad")
            .value(hasItem(DEFAULT_ESPECIALIDAD.toString()));
    }
}
