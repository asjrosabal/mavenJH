package com.mycompany.myapp.repository.rowmapper;

import com.mycompany.myapp.domain.Historia;
import com.mycompany.myapp.service.ColumnConverter;
import io.r2dbc.spi.Row;
import java.time.LocalDate;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Historia}, with proper type conversions.
 */
@Service
public class HistoriaRowMapper implements BiFunction<Row, String, Historia> {

    private final ColumnConverter converter;

    public HistoriaRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Historia} stored in the database.
     */
    @Override
    public Historia apply(Row row, String prefix) {
        Historia entity = new Historia();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setFecha(converter.fromRow(row, prefix + "_fecha", LocalDate.class));
        entity.setDiagnostico(converter.fromRow(row, prefix + "_diagnostico", String.class));
        entity.setDescripcion(converter.fromRow(row, prefix + "_descripcion", String.class));
        entity.setResultadoFile(converter.fromRow(row, prefix + "_resultado_file", String.class));
        entity.setRutId(converter.fromRow(row, prefix + "_rut_id", Long.class));
        return entity;
    }
}
