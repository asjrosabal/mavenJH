package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.myapp.domain.enumeration.Especialidad;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A Reserva.
 */
@Table("reserva")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "reserva")
public class Reserva implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Column("fecha")
    private LocalDate fecha;

    @Column("hora")
    private Instant hora;

    @Column("especialidad")
    private Especialidad especialidad;

    @JsonIgnoreProperties(value = { "historias", "reservas", "rut" }, allowSetters = true)
    @Transient
    private Paciente rut;

    @Column("rut_id")
    private Long rutId;

    // jhipster-needle-entity-add-field - JHipster will add fields here
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Reserva id(Long id) {
        this.id = id;
        return this;
    }

    public LocalDate getFecha() {
        return this.fecha;
    }

    public Reserva fecha(LocalDate fecha) {
        this.fecha = fecha;
        return this;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Instant getHora() {
        return this.hora;
    }

    public Reserva hora(Instant hora) {
        this.hora = hora;
        return this;
    }

    public void setHora(Instant hora) {
        this.hora = hora;
    }

    public Especialidad getEspecialidad() {
        return this.especialidad;
    }

    public Reserva especialidad(Especialidad especialidad) {
        this.especialidad = especialidad;
        return this;
    }

    public void setEspecialidad(Especialidad especialidad) {
        this.especialidad = especialidad;
    }

    public Paciente getRut() {
        return this.rut;
    }

    public Reserva rut(Paciente paciente) {
        this.setRut(paciente);
        this.rutId = paciente != null ? paciente.getId() : null;
        return this;
    }

    public void setRut(Paciente paciente) {
        this.rut = paciente;
        this.rutId = paciente != null ? paciente.getId() : null;
    }

    public Long getRutId() {
        return this.rutId;
    }

    public void setRutId(Long paciente) {
        this.rutId = paciente;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reserva)) {
            return false;
        }
        return id != null && id.equals(((Reserva) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Reserva{" +
            "id=" + getId() +
            ", fecha='" + getFecha() + "'" +
            ", hora='" + getHora() + "'" +
            ", especialidad='" + getEspecialidad() + "'" +
            "}";
    }
}
