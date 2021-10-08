package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A Historia.
 */
@Table("historia")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "historia")
public class Historia implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Column("fecha")
    private LocalDate fecha;

    @Column("diagnostico")
    private String diagnostico;

    @Column("descripcion")
    private String descripcion;

    @Column("resultado_file")
    private String resultadoFile;

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

    public Historia id(Long id) {
        this.id = id;
        return this;
    }

    public LocalDate getFecha() {
        return this.fecha;
    }

    public Historia fecha(LocalDate fecha) {
        this.fecha = fecha;
        return this;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getDiagnostico() {
        return this.diagnostico;
    }

    public Historia diagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
        return this;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getDescripcion() {
        return this.descripcion;
    }

    public Historia descripcion(String descripcion) {
        this.descripcion = descripcion;
        return this;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getResultadoFile() {
        return this.resultadoFile;
    }

    public Historia resultadoFile(String resultadoFile) {
        this.resultadoFile = resultadoFile;
        return this;
    }

    public void setResultadoFile(String resultadoFile) {
        this.resultadoFile = resultadoFile;
    }

    public Paciente getRut() {
        return this.rut;
    }

    public Historia rut(Paciente paciente) {
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
        if (!(o instanceof Historia)) {
            return false;
        }
        return id != null && id.equals(((Historia) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Historia{" +
            "id=" + getId() +
            ", fecha='" + getFecha() + "'" +
            ", diagnostico='" + getDiagnostico() + "'" +
            ", descripcion='" + getDescripcion() + "'" +
            ", resultadoFile='" + getResultadoFile() + "'" +
            "}";
    }
}
