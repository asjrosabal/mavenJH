package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Especialista;
import com.mycompany.myapp.domain.enumeration.Especialidad;
import com.mycompany.myapp.repository.EspecialistaRepository;
import com.mycompany.myapp.repository.search.EspecialistaSearchRepository;
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
 * Integration tests for the {@link EspecialistaResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient
@WithMockUser
class EspecialistaResourceIT {

    private static final String DEFAULT_NOMBRE = "AAAAAAAAAA";
    private static final String UPDATED_NOMBRE = "BBBBBBBBBB";

    private static final String DEFAULT_APELLIDOS = "AAAAAAAAAA";
    private static final String UPDATED_APELLIDOS = "BBBBBBBBBB";

    private static final String DEFAULT_RUT = "AAAAAAAAAA";
    private static final String UPDATED_RUT = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_FECHA_NACIMIENTO = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_FECHA_NACIMIENTO = LocalDate.now(ZoneId.systemDefault());

    private static final String DEFAULT_REGISTRO_MEDICO = "AAAAAAAAAA";
    private static final String UPDATED_REGISTRO_MEDICO = "BBBBBBBBBB";

    private static final Especialidad DEFAULT_ESPECIALIDAD = Especialidad.MEDICINA_GENERAL;
    private static final Especialidad UPDATED_ESPECIALIDAD = Especialidad.LABORATORIO;

    private static final String ENTITY_API_URL = "/api/especialistas";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/_search/especialistas";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private EspecialistaRepository especialistaRepository;

    /**
     * This repository is mocked in the com.mycompany.myapp.repository.search test package.
     *
     * @see com.mycompany.myapp.repository.search.EspecialistaSearchRepositoryMockConfiguration
     */
    @Autowired
    private EspecialistaSearchRepository mockEspecialistaSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Especialista especialista;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Especialista createEntity(EntityManager em) {
        Especialista especialista = new Especialista()
            .nombre(DEFAULT_NOMBRE)
            .apellidos(DEFAULT_APELLIDOS)
            .rut(DEFAULT_RUT)
            .fechaNacimiento(DEFAULT_FECHA_NACIMIENTO)
            .registroMedico(DEFAULT_REGISTRO_MEDICO)
            .especialidad(DEFAULT_ESPECIALIDAD);
        return especialista;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Especialista createUpdatedEntity(EntityManager em) {
        Especialista especialista = new Especialista()
            .nombre(UPDATED_NOMBRE)
            .apellidos(UPDATED_APELLIDOS)
            .rut(UPDATED_RUT)
            .fechaNacimiento(UPDATED_FECHA_NACIMIENTO)
            .registroMedico(UPDATED_REGISTRO_MEDICO)
            .especialidad(UPDATED_ESPECIALIDAD);
        return especialista;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Especialista.class).block();
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
        especialista = createEntity(em);
    }

    @Test
    void createEspecialista() throws Exception {
        int databaseSizeBeforeCreate = especialistaRepository.findAll().collectList().block().size();
        // Configure the mock search repository
        when(mockEspecialistaSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Create the Especialista
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(especialista))
            .exchange()
            .expectStatus()
            .isCreated();

        // Validate the Especialista in the database
        List<Especialista> especialistaList = especialistaRepository.findAll().collectList().block();
        assertThat(especialistaList).hasSize(databaseSizeBeforeCreate + 1);
        Especialista testEspecialista = especialistaList.get(especialistaList.size() - 1);
        assertThat(testEspecialista.getNombre()).isEqualTo(DEFAULT_NOMBRE);
        assertThat(testEspecialista.getApellidos()).isEqualTo(DEFAULT_APELLIDOS);
        assertThat(testEspecialista.getRut()).isEqualTo(DEFAULT_RUT);
        assertThat(testEspecialista.getFechaNacimiento()).isEqualTo(DEFAULT_FECHA_NACIMIENTO);
        assertThat(testEspecialista.getRegistroMedico()).isEqualTo(DEFAULT_REGISTRO_MEDICO);
        assertThat(testEspecialista.getEspecialidad()).isEqualTo(DEFAULT_ESPECIALIDAD);

        // Validate the Especialista in Elasticsearch
        verify(mockEspecialistaSearchRepository, times(1)).save(testEspecialista);
    }

    @Test
    void createEspecialistaWithExistingId() throws Exception {
        // Create the Especialista with an existing ID
        especialista.setId(1L);

        int databaseSizeBeforeCreate = especialistaRepository.findAll().collectList().block().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(especialista))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Especialista in the database
        List<Especialista> especialistaList = especialistaRepository.findAll().collectList().block();
        assertThat(especialistaList).hasSize(databaseSizeBeforeCreate);

        // Validate the Especialista in Elasticsearch
        verify(mockEspecialistaSearchRepository, times(0)).save(especialista);
    }

    @Test
    void getAllEspecialistasAsStream() {
        // Initialize the database
        especialistaRepository.save(especialista).block();

        List<Especialista> especialistaList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(Especialista.class)
            .getResponseBody()
            .filter(especialista::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(especialistaList).isNotNull();
        assertThat(especialistaList).hasSize(1);
        Especialista testEspecialista = especialistaList.get(0);
        assertThat(testEspecialista.getNombre()).isEqualTo(DEFAULT_NOMBRE);
        assertThat(testEspecialista.getApellidos()).isEqualTo(DEFAULT_APELLIDOS);
        assertThat(testEspecialista.getRut()).isEqualTo(DEFAULT_RUT);
        assertThat(testEspecialista.getFechaNacimiento()).isEqualTo(DEFAULT_FECHA_NACIMIENTO);
        assertThat(testEspecialista.getRegistroMedico()).isEqualTo(DEFAULT_REGISTRO_MEDICO);
        assertThat(testEspecialista.getEspecialidad()).isEqualTo(DEFAULT_ESPECIALIDAD);
    }

    @Test
    void getAllEspecialistas() {
        // Initialize the database
        especialistaRepository.save(especialista).block();

        // Get all the especialistaList
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
            .value(hasItem(especialista.getId().intValue()))
            .jsonPath("$.[*].nombre")
            .value(hasItem(DEFAULT_NOMBRE))
            .jsonPath("$.[*].apellidos")
            .value(hasItem(DEFAULT_APELLIDOS))
            .jsonPath("$.[*].rut")
            .value(hasItem(DEFAULT_RUT))
            .jsonPath("$.[*].fechaNacimiento")
            .value(hasItem(DEFAULT_FECHA_NACIMIENTO.toString()))
            .jsonPath("$.[*].registroMedico")
            .value(hasItem(DEFAULT_REGISTRO_MEDICO))
            .jsonPath("$.[*].especialidad")
            .value(hasItem(DEFAULT_ESPECIALIDAD.toString()));
    }

    @Test
    void getEspecialista() {
        // Initialize the database
        especialistaRepository.save(especialista).block();

        // Get the especialista
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, especialista.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(especialista.getId().intValue()))
            .jsonPath("$.nombre")
            .value(is(DEFAULT_NOMBRE))
            .jsonPath("$.apellidos")
            .value(is(DEFAULT_APELLIDOS))
            .jsonPath("$.rut")
            .value(is(DEFAULT_RUT))
            .jsonPath("$.fechaNacimiento")
            .value(is(DEFAULT_FECHA_NACIMIENTO.toString()))
            .jsonPath("$.registroMedico")
            .value(is(DEFAULT_REGISTRO_MEDICO))
            .jsonPath("$.especialidad")
            .value(is(DEFAULT_ESPECIALIDAD.toString()));
    }

    @Test
    void getNonExistingEspecialista() {
        // Get the especialista
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putNewEspecialista() throws Exception {
        // Configure the mock search repository
        when(mockEspecialistaSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Initialize the database
        especialistaRepository.save(especialista).block();

        int databaseSizeBeforeUpdate = especialistaRepository.findAll().collectList().block().size();

        // Update the especialista
        Especialista updatedEspecialista = especialistaRepository.findById(especialista.getId()).block();
        updatedEspecialista
            .nombre(UPDATED_NOMBRE)
            .apellidos(UPDATED_APELLIDOS)
            .rut(UPDATED_RUT)
            .fechaNacimiento(UPDATED_FECHA_NACIMIENTO)
            .registroMedico(UPDATED_REGISTRO_MEDICO)
            .especialidad(UPDATED_ESPECIALIDAD);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedEspecialista.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(updatedEspecialista))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Especialista in the database
        List<Especialista> especialistaList = especialistaRepository.findAll().collectList().block();
        assertThat(especialistaList).hasSize(databaseSizeBeforeUpdate);
        Especialista testEspecialista = especialistaList.get(especialistaList.size() - 1);
        assertThat(testEspecialista.getNombre()).isEqualTo(UPDATED_NOMBRE);
        assertThat(testEspecialista.getApellidos()).isEqualTo(UPDATED_APELLIDOS);
        assertThat(testEspecialista.getRut()).isEqualTo(UPDATED_RUT);
        assertThat(testEspecialista.getFechaNacimiento()).isEqualTo(UPDATED_FECHA_NACIMIENTO);
        assertThat(testEspecialista.getRegistroMedico()).isEqualTo(UPDATED_REGISTRO_MEDICO);
        assertThat(testEspecialista.getEspecialidad()).isEqualTo(UPDATED_ESPECIALIDAD);

        // Validate the Especialista in Elasticsearch
        verify(mockEspecialistaSearchRepository).save(testEspecialista);
    }

    @Test
    void putNonExistingEspecialista() throws Exception {
        int databaseSizeBeforeUpdate = especialistaRepository.findAll().collectList().block().size();
        especialista.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, especialista.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(especialista))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Especialista in the database
        List<Especialista> especialistaList = especialistaRepository.findAll().collectList().block();
        assertThat(especialistaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Especialista in Elasticsearch
        verify(mockEspecialistaSearchRepository, times(0)).save(especialista);
    }

    @Test
    void putWithIdMismatchEspecialista() throws Exception {
        int databaseSizeBeforeUpdate = especialistaRepository.findAll().collectList().block().size();
        especialista.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(especialista))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Especialista in the database
        List<Especialista> especialistaList = especialistaRepository.findAll().collectList().block();
        assertThat(especialistaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Especialista in Elasticsearch
        verify(mockEspecialistaSearchRepository, times(0)).save(especialista);
    }

    @Test
    void putWithMissingIdPathParamEspecialista() throws Exception {
        int databaseSizeBeforeUpdate = especialistaRepository.findAll().collectList().block().size();
        especialista.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(especialista))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Especialista in the database
        List<Especialista> especialistaList = especialistaRepository.findAll().collectList().block();
        assertThat(especialistaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Especialista in Elasticsearch
        verify(mockEspecialistaSearchRepository, times(0)).save(especialista);
    }

    @Test
    void partialUpdateEspecialistaWithPatch() throws Exception {
        // Initialize the database
        especialistaRepository.save(especialista).block();

        int databaseSizeBeforeUpdate = especialistaRepository.findAll().collectList().block().size();

        // Update the especialista using partial update
        Especialista partialUpdatedEspecialista = new Especialista();
        partialUpdatedEspecialista.setId(especialista.getId());

        partialUpdatedEspecialista
            .apellidos(UPDATED_APELLIDOS)
            .fechaNacimiento(UPDATED_FECHA_NACIMIENTO)
            .especialidad(UPDATED_ESPECIALIDAD);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedEspecialista.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedEspecialista))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Especialista in the database
        List<Especialista> especialistaList = especialistaRepository.findAll().collectList().block();
        assertThat(especialistaList).hasSize(databaseSizeBeforeUpdate);
        Especialista testEspecialista = especialistaList.get(especialistaList.size() - 1);
        assertThat(testEspecialista.getNombre()).isEqualTo(DEFAULT_NOMBRE);
        assertThat(testEspecialista.getApellidos()).isEqualTo(UPDATED_APELLIDOS);
        assertThat(testEspecialista.getRut()).isEqualTo(DEFAULT_RUT);
        assertThat(testEspecialista.getFechaNacimiento()).isEqualTo(UPDATED_FECHA_NACIMIENTO);
        assertThat(testEspecialista.getRegistroMedico()).isEqualTo(DEFAULT_REGISTRO_MEDICO);
        assertThat(testEspecialista.getEspecialidad()).isEqualTo(UPDATED_ESPECIALIDAD);
    }

    @Test
    void fullUpdateEspecialistaWithPatch() throws Exception {
        // Initialize the database
        especialistaRepository.save(especialista).block();

        int databaseSizeBeforeUpdate = especialistaRepository.findAll().collectList().block().size();

        // Update the especialista using partial update
        Especialista partialUpdatedEspecialista = new Especialista();
        partialUpdatedEspecialista.setId(especialista.getId());

        partialUpdatedEspecialista
            .nombre(UPDATED_NOMBRE)
            .apellidos(UPDATED_APELLIDOS)
            .rut(UPDATED_RUT)
            .fechaNacimiento(UPDATED_FECHA_NACIMIENTO)
            .registroMedico(UPDATED_REGISTRO_MEDICO)
            .especialidad(UPDATED_ESPECIALIDAD);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedEspecialista.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedEspecialista))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Especialista in the database
        List<Especialista> especialistaList = especialistaRepository.findAll().collectList().block();
        assertThat(especialistaList).hasSize(databaseSizeBeforeUpdate);
        Especialista testEspecialista = especialistaList.get(especialistaList.size() - 1);
        assertThat(testEspecialista.getNombre()).isEqualTo(UPDATED_NOMBRE);
        assertThat(testEspecialista.getApellidos()).isEqualTo(UPDATED_APELLIDOS);
        assertThat(testEspecialista.getRut()).isEqualTo(UPDATED_RUT);
        assertThat(testEspecialista.getFechaNacimiento()).isEqualTo(UPDATED_FECHA_NACIMIENTO);
        assertThat(testEspecialista.getRegistroMedico()).isEqualTo(UPDATED_REGISTRO_MEDICO);
        assertThat(testEspecialista.getEspecialidad()).isEqualTo(UPDATED_ESPECIALIDAD);
    }

    @Test
    void patchNonExistingEspecialista() throws Exception {
        int databaseSizeBeforeUpdate = especialistaRepository.findAll().collectList().block().size();
        especialista.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, especialista.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(especialista))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Especialista in the database
        List<Especialista> especialistaList = especialistaRepository.findAll().collectList().block();
        assertThat(especialistaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Especialista in Elasticsearch
        verify(mockEspecialistaSearchRepository, times(0)).save(especialista);
    }

    @Test
    void patchWithIdMismatchEspecialista() throws Exception {
        int databaseSizeBeforeUpdate = especialistaRepository.findAll().collectList().block().size();
        especialista.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(especialista))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Especialista in the database
        List<Especialista> especialistaList = especialistaRepository.findAll().collectList().block();
        assertThat(especialistaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Especialista in Elasticsearch
        verify(mockEspecialistaSearchRepository, times(0)).save(especialista);
    }

    @Test
    void patchWithMissingIdPathParamEspecialista() throws Exception {
        int databaseSizeBeforeUpdate = especialistaRepository.findAll().collectList().block().size();
        especialista.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(especialista))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Especialista in the database
        List<Especialista> especialistaList = especialistaRepository.findAll().collectList().block();
        assertThat(especialistaList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Especialista in Elasticsearch
        verify(mockEspecialistaSearchRepository, times(0)).save(especialista);
    }

    @Test
    void deleteEspecialista() {
        // Configure the mock search repository
        when(mockEspecialistaSearchRepository.deleteById(anyLong())).thenReturn(Mono.empty());
        // Initialize the database
        especialistaRepository.save(especialista).block();

        int databaseSizeBeforeDelete = especialistaRepository.findAll().collectList().block().size();

        // Delete the especialista
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, especialista.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        List<Especialista> especialistaList = especialistaRepository.findAll().collectList().block();
        assertThat(especialistaList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the Especialista in Elasticsearch
        verify(mockEspecialistaSearchRepository, times(1)).deleteById(especialista.getId());
    }

    @Test
    void searchEspecialista() {
        // Configure the mock search repository
        when(mockEspecialistaSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Initialize the database
        especialistaRepository.save(especialista).block();
        when(mockEspecialistaSearchRepository.search("id:" + especialista.getId())).thenReturn(Flux.just(especialista));

        // Search the especialista
        webTestClient
            .get()
            .uri(ENTITY_SEARCH_API_URL + "?query=id:" + especialista.getId())
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(especialista.getId().intValue()))
            .jsonPath("$.[*].nombre")
            .value(hasItem(DEFAULT_NOMBRE))
            .jsonPath("$.[*].apellidos")
            .value(hasItem(DEFAULT_APELLIDOS))
            .jsonPath("$.[*].rut")
            .value(hasItem(DEFAULT_RUT))
            .jsonPath("$.[*].fechaNacimiento")
            .value(hasItem(DEFAULT_FECHA_NACIMIENTO.toString()))
            .jsonPath("$.[*].registroMedico")
            .value(hasItem(DEFAULT_REGISTRO_MEDICO))
            .jsonPath("$.[*].especialidad")
            .value(hasItem(DEFAULT_ESPECIALIDAD.toString()));
    }
}
