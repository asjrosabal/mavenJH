package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A Paciente.
 */
@Table("paciente")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "paciente")
public class Paciente implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Column("nombre")
    private String nombre;

    @Column("apellidos")
    private String apellidos;

    @Column("rut")
    private String rut;

    @Column("fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Transient
    @JsonIgnoreProperties(value = { "rut" }, allowSetters = true)
    private Set<Historia> historias = new HashSet<>();

    @Transient
    @JsonIgnoreProperties(value = { "rut" }, allowSetters = true)
    private Set<Reserva> reservas = new HashSet<>();

    @JsonIgnoreProperties(value = { "pacientes" }, allowSetters = true)
    @Transient
    private Especialista rut;

    @Column("rut_id")
    private Long rutId;

    // jhipster-needle-entity-add-field - JHipster will add fields here
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Paciente id(Long id) {
        this.id = id;
        return this;
    }

    public String getNombre() {
        return this.nombre;
    }

    public Paciente nombre(String nombre) {
        this.nombre = nombre;
        return this;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return this.apellidos;
    }

    public Paciente apellidos(String apellidos) {
        this.apellidos = apellidos;
        return this;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getRut() {
        return this.rut;
    }

    public Paciente rut(String rut) {
        this.rut = rut;
        return this;
    }

    public void setRut(String rut) {
        this.rut = rut;
    }

    public LocalDate getFechaNacimiento() {
        return this.fechaNacimiento;
    }

    public Paciente fechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
        return this;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public Set<Historia> getHistorias() {
        return this.historias;
    }

    public Paciente historias(Set<Historia> historias) {
        this.setHistorias(historias);
        return this;
    }

    public Paciente addHistoria(Historia historia) {
        this.historias.add(historia);
        historia.setRut(this);
        return this;
    }

    public Paciente removeHistoria(Historia historia) {
        this.historias.remove(historia);
        historia.setRut(null);
        return this;
    }

    public void setHistorias(Set<Historia> historias) {
        if (this.historias != null) {
            this.historias.forEach(i -> i.setRut(null));
        }
        if (historias != null) {
            historias.forEach(i -> i.setRut(this));
        }
        this.historias = historias;
    }

    public Set<Reserva> getReservas() {
        return this.reservas;
    }

    public Paciente reservas(Set<Reserva> reservas) {
        this.setReservas(reservas);
        return this;
    }

    public Paciente addReserva(Reserva reserva) {
        this.reservas.add(reserva);
        reserva.setRut(this);
        return this;
    }

    public Paciente removeReserva(Reserva reserva) {
        this.reservas.remove(reserva);
        reserva.setRut(null);
        return this;
    }

    public void setReservas(Set<Reserva> reservas) {
        if (this.reservas != null) {
            this.reservas.forEach(i -> i.setRut(null));
        }
        if (reservas != null) {
            reservas.forEach(i -> i.setRut(this));
        }
        this.reservas = reservas;
    }

    public Especialista getRut() {
        return this.rut;
    }

    public Paciente rut(Especialista especialista) {
        this.setRut(especialista);
        this.rutId = especialista != null ? especialista.getId() : null;
        return this;
    }

    public void setRut(Especialista especialista) {
        this.rut = especialista;
        this.rutId = especialista != null ? especialista.getId() : null;
    }

    public Long getRutId() {
        return this.rutId;
    }

    public void setRutId(Long especialista) {
        this.rutId = especialista;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Paciente)) {
            return false;
        }
        return id != null && id.equals(((Paciente) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Paciente{" +
            "id=" + getId() +
            ", nombre='" + getNombre() + "'" +
            ", apellidos='" + getApellidos() + "'" +
            ", rut='" + getRut() + "'" +
            ", fechaNacimiento='" + getFechaNacimiento() + "'" +
            "}";
    }
}
