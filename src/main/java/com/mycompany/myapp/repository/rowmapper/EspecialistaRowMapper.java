package com.mycompany.myapp.repository.rowmapper;

import com.mycompany.myapp.domain.Especialista;
import com.mycompany.myapp.domain.enumeration.Especialidad;
import com.mycompany.myapp.service.ColumnConverter;
import io.r2dbc.spi.Row;
import java.time.LocalDate;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Especialista}, with proper type conversions.
 */
@Service
public class EspecialistaRowMapper implements BiFunction<Row, String, Especialista> {

    private final ColumnConverter converter;

    public EspecialistaRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Especialista} stored in the database.
     */
    @Override
    public Especialista apply(Row row, String prefix) {
        Especialista entity = new Especialista();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setNombre(converter.fromRow(row, prefix + "_nombre", String.class));
        entity.setApellidos(converter.fromRow(row, prefix + "_apellidos", String.class));
        entity.setRut(converter.fromRow(row, prefix + "_rut", String.class));
        entity.setFechaNacimiento(converter.fromRow(row, prefix + "_fecha_nacimiento", LocalDate.class));
        entity.setRegistroMedico(converter.fromRow(row, prefix + "_registro_medico", String.class));
        entity.setEspecialidad(converter.fromRow(row, prefix + "_especialidad", Especialidad.class));
        return entity;
    }
}
