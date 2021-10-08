package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Paciente;
import com.mycompany.myapp.repository.PacienteRepository;
import com.mycompany.myapp.repository.search.PacienteSearchRepository;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Integration tests for the {@link PacienteResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient
@WithMockUser
class PacienteResourceIT {

    private static final String DEFAULT_NOMBRE = "AAAAAAAAAA";
    private static final String UPDATED_NOMBRE = "BBBBBBBBBB";

    private static final String DEFAULT_APELLIDOS = "AAAAAAAAAA";
    private static final String UPDATED_APELLIDOS = "BBBBBBBBBB";

    private static final String DEFAULT_RUT = "AAAAAAAAAA";
    private static final String UPDATED_RUT = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_FECHA_NACIMIENTO = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_FECHA_NACIMIENTO = LocalDate.now(ZoneId.systemDefault());

    private static final String ENTITY_API_URL = "/api/pacientes";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/_search/pacientes";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private PacienteRepository pacienteRepository;

    /**
     * This repository is mocked in the com.mycompany.myapp.repository.search test package.
     *
     * @see com.mycompany.myapp.repository.search.PacienteSearchRepositoryMockConfiguration
     */
    @Autowired
    private PacienteSearchRepository mockPacienteSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Paciente paciente;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Paciente createEntity(EntityManager em) {
        Paciente paciente = new Paciente()
            .nombre(DEFAULT_NOMBRE)
            .apellidos(DEFAULT_APELLIDOS)
            .rut(DEFAULT_RUT)
            .fechaNacimiento(DEFAULT_FECHA_NACIMIENTO);
        return paciente;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Paciente createUpdatedEntity(EntityManager em) {
        Paciente paciente = new Paciente()
            .nombre(UPDATED_NOMBRE)
            .apellidos(UPDATED_APELLIDOS)
            .rut(UPDATED_RUT)
            .fechaNacimiento(UPDATED_FECHA_NACIMIENTO);
        return paciente;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Paciente.class).block();
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
        paciente = createEntity(em);
    }

    @Test
    void createPaciente() throws Exception {
        int databaseSizeBeforeCreate = pacienteRepository.findAll().collectList().block().size();
        // Configure the mock search repository
        when(mockPacienteSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Create the Paciente
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(paciente))
            .exchange()
            .expectStatus()
            .isCreated();

        // Validate the Paciente in the database
        List<Paciente> pacienteList = pacienteRepository.findAll().collectList().block();
        assertThat(pacienteList).hasSize(databaseSizeBeforeCreate + 1);
        Paciente testPaciente = pacienteList.get(pacienteList.size() - 1);
        assertThat(testPaciente.getNombre()).isEqualTo(DEFAULT_NOMBRE);
        assertThat(testPaciente.getApellidos()).isEqualTo(DEFAULT_APELLIDOS);
        assertThat(testPaciente.getRut()).isEqualTo(DEFAULT_RUT);
        assertThat(testPaciente.getFechaNacimiento()).isEqualTo(DEFAULT_FECHA_NACIMIENTO);

        // Validate the Paciente in Elasticsearch
        verify(mockPacienteSearchRepository, times(1)).save(testPaciente);
    }

    @Test
    void createPacienteWithExistingId() throws Exception {
        // Create the Paciente with an existing ID
        paciente.setId(1L);

        int databaseSizeBeforeCreate = pacienteRepository.findAll().collectList().block().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(paciente))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Paciente in the database
        List<Paciente> pacienteList = pacienteRepository.findAll().collectList().block();
        assertThat(pacienteList).hasSize(databaseSizeBeforeCreate);

        // Validate the Paciente in Elasticsearch
        verify(mockPacienteSearchRepository, times(0)).save(paciente);
    }

    @Test
    void getAllPacientesAsStream() {
        // Initialize the database
        pacienteRepository.save(paciente).block();

        List<Paciente> pacienteList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(Paciente.class)
            .getResponseBody()
            .filter(paciente::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(pacienteList).isNotNull();
        assertThat(pacienteList).hasSize(1);
        Paciente testPaciente = pacienteList.get(0);
        assertThat(testPaciente.getNombre()).isEqualTo(DEFAULT_NOMBRE);
        assertThat(testPaciente.getApellidos()).isEqualTo(DEFAULT_APELLIDOS);
        assertThat(testPaciente.getRut()).isEqualTo(DEFAULT_RUT);
        assertThat(testPaciente.getFechaNacimiento()).isEqualTo(DEFAULT_FECHA_NACIMIENTO);
    }

    @Test
    void getAllPacientes() {
        // Initialize the database
        pacienteRepository.save(paciente).block();

        // Get all the pacienteList
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
            .value(hasItem(paciente.getId().intValue()))
            .jsonPath("$.[*].nombre")
            .value(hasItem(DEFAULT_NOMBRE))
            .jsonPath("$.[*].apellidos")
            .value(hasItem(DEFAULT_APELLIDOS))
            .jsonPath("$.[*].rut")
            .value(hasItem(DEFAULT_RUT))
            .jsonPath("$.[*].fechaNacimiento")
            .value(hasItem(DEFAULT_FECHA_NACIMIENTO.toString()));
    }

    @Test
    void getPaciente() {
        // Initialize the database
        pacienteRepository.save(paciente).block();

        // Get the paciente
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, paciente.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(paciente.getId().intValue()))
            .jsonPath("$.nombre")
            .value(is(DEFAULT_NOMBRE))
            .jsonPath("$.apellidos")
            .value(is(DEFAULT_APELLIDOS))
            .jsonPath("$.rut")
            .value(is(DEFAULT_RUT))
            .jsonPath("$.fechaNacimiento")
            .value(is(DEFAULT_FECHA_NACIMIENTO.toString()));
    }

    @Test
    void getNonExistingPaciente() {
        // Get the paciente
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putNewPaciente() throws Exception {
        // Configure the mock search repository
        when(mockPacienteSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Initialize the database
        pacienteRepository.save(paciente).block();

        int databaseSizeBeforeUpdate = pacienteRepository.findAll().collectList().block().size();

        // Update the paciente
        Paciente updatedPaciente = pacienteRepository.findById(paciente.getId()).block();
        updatedPaciente.nombre(UPDATED_NOMBRE).apellidos(UPDATED_APELLIDOS).rut(UPDATED_RUT).fechaNacimiento(UPDATED_FECHA_NACIMIENTO);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedPaciente.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(updatedPaciente))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Paciente in the database
        List<Paciente> pacienteList = pacienteRepository.findAll().collectList().block();
        assertThat(pacienteList).hasSize(databaseSizeBeforeUpdate);
        Paciente testPaciente = pacienteList.get(pacienteList.size() - 1);
        assertThat(testPaciente.getNombre()).isEqualTo(UPDATED_NOMBRE);
        assertThat(testPaciente.getApellidos()).isEqualTo(UPDATED_APELLIDOS);
        assertThat(testPaciente.getRut()).isEqualTo(UPDATED_RUT);
        assertThat(testPaciente.getFechaNacimiento()).isEqualTo(UPDATED_FECHA_NACIMIENTO);

        // Validate the Paciente in Elasticsearch
        verify(mockPacienteSearchRepository).save(testPaciente);
    }

    @Test
    void putNonExistingPaciente() throws Exception {
        int databaseSizeBeforeUpdate = pacienteRepository.findAll().collectList().block().size();
        paciente.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, paciente.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(paciente))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Paciente in the database
        List<Paciente> pacienteList = pacienteRepository.findAll().collectList().block();
        assertThat(pacienteList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Paciente in Elasticsearch
        verify(mockPacienteSearchRepository, times(0)).save(paciente);
    }

    @Test
    void putWithIdMismatchPaciente() throws Exception {
        int databaseSizeBeforeUpdate = pacienteRepository.findAll().collectList().block().size();
        paciente.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(paciente))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Paciente in the database
        List<Paciente> pacienteList = pacienteRepository.findAll().collectList().block();
        assertThat(pacienteList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Paciente in Elasticsearch
        verify(mockPacienteSearchRepository, times(0)).save(paciente);
    }

    @Test
    void putWithMissingIdPathParamPaciente() throws Exception {
        int databaseSizeBeforeUpdate = pacienteRepository.findAll().collectList().block().size();
        paciente.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(paciente))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Paciente in the database
        List<Paciente> pacienteList = pacienteRepository.findAll().collectList().block();
        assertThat(pacienteList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Paciente in Elasticsearch
        verify(mockPacienteSearchRepository, times(0)).save(paciente);
    }

    @Test
    void partialUpdatePacienteWithPatch() throws Exception {
        // Initialize the database
        pacienteRepository.save(paciente).block();

        int databaseSizeBeforeUpdate = pacienteRepository.findAll().collectList().block().size();

        // Update the paciente using partial update
        Paciente partialUpdatedPaciente = new Paciente();
        partialUpdatedPaciente.setId(paciente.getId());

        partialUpdatedPaciente.nombre(UPDATED_NOMBRE).rut(UPDATED_RUT);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedPaciente.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedPaciente))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Paciente in the database
        List<Paciente> pacienteList = pacienteRepository.findAll().collectList().block();
        assertThat(pacienteList).hasSize(databaseSizeBeforeUpdate);
        Paciente testPaciente = pacienteList.get(pacienteList.size() - 1);
        assertThat(testPaciente.getNombre()).isEqualTo(UPDATED_NOMBRE);
        assertThat(testPaciente.getApellidos()).isEqualTo(DEFAULT_APELLIDOS);
        assertThat(testPaciente.getRut()).isEqualTo(UPDATED_RUT);
        assertThat(testPaciente.getFechaNacimiento()).isEqualTo(DEFAULT_FECHA_NACIMIENTO);
    }

    @Test
    void fullUpdatePacienteWithPatch() throws Exception {
        // Initialize the database
        pacienteRepository.save(paciente).block();

        int databaseSizeBeforeUpdate = pacienteRepository.findAll().collectList().block().size();

        // Update the paciente using partial update
        Paciente partialUpdatedPaciente = new Paciente();
        partialUpdatedPaciente.setId(paciente.getId());

        partialUpdatedPaciente
            .nombre(UPDATED_NOMBRE)
            .apellidos(UPDATED_APELLIDOS)
            .rut(UPDATED_RUT)
            .fechaNacimiento(UPDATED_FECHA_NACIMIENTO);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedPaciente.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedPaciente))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Paciente in the database
        List<Paciente> pacienteList = pacienteRepository.findAll().collectList().block();
        assertThat(pacienteList).hasSize(databaseSizeBeforeUpdate);
        Paciente testPaciente = pacienteList.get(pacienteList.size() - 1);
        assertThat(testPaciente.getNombre()).isEqualTo(UPDATED_NOMBRE);
        assertThat(testPaciente.getApellidos()).isEqualTo(UPDATED_APELLIDOS);
        assertThat(testPaciente.getRut()).isEqualTo(UPDATED_RUT);
        assertThat(testPaciente.getFechaNacimiento()).isEqualTo(UPDATED_FECHA_NACIMIENTO);
    }

    @Test
    void patchNonExistingPaciente() throws Exception {
        int databaseSizeBeforeUpdate = pacienteRepository.findAll().collectList().block().size();
        paciente.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, paciente.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(paciente))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Paciente in the database
        List<Paciente> pacienteList = pacienteRepository.findAll().collectList().block();
        assertThat(pacienteList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Paciente in Elasticsearch
        verify(mockPacienteSearchRepository, times(0)).save(paciente);
    }

    @Test
    void patchWithIdMismatchPaciente() throws Exception {
        int databaseSizeBeforeUpdate = pacienteRepository.findAll().collectList().block().size();
        paciente.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(paciente))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Paciente in the database
        List<Paciente> pacienteList = pacienteRepository.findAll().collectList().block();
        assertThat(pacienteList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Paciente in Elasticsearch
        verify(mockPacienteSearchRepository, times(0)).save(paciente);
    }

    @Test
    void patchWithMissingIdPathParamPaciente() throws Exception {
        int databaseSizeBeforeUpdate = pacienteRepository.findAll().collectList().block().size();
        paciente.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(paciente))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Paciente in the database
        List<Paciente> pacienteList = pacienteRepository.findAll().collectList().block();
        assertThat(pacienteList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Paciente in Elasticsearch
        verify(mockPacienteSearchRepository, times(0)).save(paciente);
    }

    @Test
    void deletePaciente() {
        // Configure the mock search repository
        when(mockPacienteSearchRepository.deleteById(anyLong())).thenReturn(Mono.empty());
        // Initialize the database
        pacienteRepository.save(paciente).block();

        int databaseSizeBeforeDelete = pacienteRepository.findAll().collectList().block().size();

        // Delete the paciente
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, paciente.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        List<Paciente> pacienteList = pacienteRepository.findAll().collectList().block();
        assertThat(pacienteList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the Paciente in Elasticsearch
        verify(mockPacienteSearchRepository, times(1)).deleteById(paciente.getId());
    }

    @Test
    void searchPaciente() {
        // Configure the mock search repository
        when(mockPacienteSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Initialize the database
        pacienteRepository.save(paciente).block();
        when(mockPacienteSearchRepository.search("id:" + paciente.getId())).thenReturn(Flux.just(paciente));

        // Search the paciente
        webTestClient
            .get()
            .uri(ENTITY_SEARCH_API_URL + "?query=id:" + paciente.getId())
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(paciente.getId().intValue()))
            .jsonPath("$.[*].nombre")
            .value(hasItem(DEFAULT_NOMBRE))
            .jsonPath("$.[*].apellidos")
            .value(hasItem(DEFAULT_APELLIDOS))
            .jsonPath("$.[*].rut")
            .value(hasItem(DEFAULT_RUT))
            .jsonPath("$.[*].fechaNacimiento")
            .value(hasItem(DEFAULT_FECHA_NACIMIENTO.toString()));
    }
}
