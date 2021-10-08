package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.myapp.domain.enumeration.Especialidad;
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
 * A Especialista.
 */
@Table("especialista")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "especialista")
public class Especialista implements Serializable {

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

    @Column("registro_medico")
    private String registroMedico;

    @Column("especialidad")
    private Especialidad especialidad;

    @Transient
    @JsonIgnoreProperties(value = { "historias", "reservas", "rut" }, allowSetters = true)
    private Set<Paciente> pacientes = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Especialista id(Long id) {
        this.id = id;
        return this;
    }

    public String getNombre() {
        return this.nombre;
    }

    public Especialista nombre(String nombre) {
        this.nombre = nombre;
        return this;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return this.apellidos;
    }

    public Especialista apellidos(String apellidos) {
        this.apellidos = apellidos;
        return this;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getRut() {
        return this.rut;
    }

    public Especialista rut(String rut) {
        this.rut = rut;
        return this;
    }

    public void setRut(String rut) {
        this.rut = rut;
    }

    public LocalDate getFechaNacimiento() {
        return this.fechaNacimiento;
    }

    public Especialista fechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
        return this;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getRegistroMedico() {
        return this.registroMedico;
    }

    public Especialista registroMedico(String registroMedico) {
        this.registroMedico = registroMedico;
        return this;
    }

    public void setRegistroMedico(String registroMedico) {
        this.registroMedico = registroMedico;
    }

    public Especialidad getEspecialidad() {
        return this.especialidad;
    }

    public Especialista especialidad(Especialidad especialidad) {
        this.especialidad = especialidad;
        return this;
    }

    public void setEspecialidad(Especialidad especialidad) {
        this.especialidad = especialidad;
    }

    public Set<Paciente> getPacientes() {
        return this.pacientes;
    }

    public Especialista pacientes(Set<Paciente> pacientes) {
        this.setPacientes(pacientes);
        return this;
    }

    public Especialista addPaciente(Paciente paciente) {
        this.pacientes.add(paciente);
        paciente.setRut(this);
        return this;
    }

    public Especialista removePaciente(Paciente paciente) {
        this.pacientes.remove(paciente);
        paciente.setRut(null);
        return this;
    }

    public void setPacientes(Set<Paciente> pacientes) {
        if (this.pacientes != null) {
            this.pacientes.forEach(i -> i.setRut(null));
        }
        if (pacientes != null) {
            pacientes.forEach(i -> i.setRut(this));
        }
        this.pacientes = pacientes;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Especialista)) {
            return false;
        }
        return id != null && id.equals(((Especialista) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Especialista{" +
            "id=" + getId() +
            ", nombre='" + getNombre() + "'" +
            ", apellidos='" + getApellidos() + "'" +
            ", rut='" + getRut() + "'" +
            ", fechaNacimiento='" + getFechaNacimiento() + "'" +
            ", registroMedico='" + getRegistroMedico() + "'" +
            ", especialidad='" + getEspecialidad() + "'" +
            "}";
    }
}
