package com.mycompany.myapp.repository.rowmapper;

import com.mycompany.myapp.domain.Reserva;
import com.mycompany.myapp.domain.enumeration.Especialidad;
import com.mycompany.myapp.service.ColumnConverter;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.time.LocalDate;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Reserva}, with proper type conversions.
 */
@Service
public class ReservaRowMapper implements BiFunction<Row, String, Reserva> {

    private final ColumnConverter converter;

    public ReservaRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Reserva} stored in the database.
     */
    @Override
    public Reserva apply(Row row, String prefix) {
        Reserva entity = new Reserva();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setFecha(converter.fromRow(row, prefix + "_fecha", LocalDate.class));
        entity.setHora(converter.fromRow(row, prefix + "_hora", Instant.class));
        entity.setEspecialidad(converter.fromRow(row, prefix + "_especialidad", Especialidad.class));
        entity.setRutId(converter.fromRow(row, prefix + "_rut_id", Long.class));
        return entity;
    }
}
