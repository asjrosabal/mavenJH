package com.mycompany.myapp.repository.rowmapper;

import com.mycompany.myapp.domain.Paciente;
import com.mycompany.myapp.service.ColumnConverter;
import io.r2dbc.spi.Row;
import java.time.LocalDate;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Paciente}, with proper type conversions.
 */
@Service
public class PacienteRowMapper implements BiFunction<Row, String, Paciente> {

    private final ColumnConverter converter;

    public PacienteRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Paciente} stored in the database.
     */
    @Override
    public Paciente apply(Row row, String prefix) {
        Paciente entity = new Paciente();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setNombre(converter.fromRow(row, prefix + "_nombre", String.class));
        entity.setApellidos(converter.fromRow(row, prefix + "_apellidos", String.class));
        entity.setRut(converter.fromRow(row, prefix + "_rut", String.class));
        entity.setFechaNacimiento(converter.fromRow(row, prefix + "_fecha_nacimiento", LocalDate.class));
        entity.setRutId(converter.fromRow(row, prefix + "_rut_id", Long.class));
        return entity;
    }
}
