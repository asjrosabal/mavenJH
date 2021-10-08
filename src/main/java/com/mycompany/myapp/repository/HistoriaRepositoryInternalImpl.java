package com.mycompany.myapp.repository;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import com.mycompany.myapp.domain.Historia;
import com.mycompany.myapp.repository.rowmapper.HistoriaRowMapper;
import com.mycompany.myapp.repository.rowmapper.PacienteRowMapper;
import com.mycompany.myapp.service.EntityManager;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectFromAndJoinCondition;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data SQL reactive custom repository implementation for the Historia entity.
 */
@SuppressWarnings("unused")
class HistoriaRepositoryInternalImpl implements HistoriaRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final PacienteRowMapper pacienteMapper;
    private final HistoriaRowMapper historiaMapper;

    private static final Table entityTable = Table.aliased("historia", EntityManager.ENTITY_ALIAS);
    private static final Table rutTable = Table.aliased("paciente", "rut");

    public HistoriaRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        PacienteRowMapper pacienteMapper,
        HistoriaRowMapper historiaMapper
    ) {
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.pacienteMapper = pacienteMapper;
        this.historiaMapper = historiaMapper;
    }

    @Override
    public Flux<Historia> findAllBy(Pageable pageable) {
        return findAllBy(pageable, null);
    }

    @Override
    public Flux<Historia> findAllBy(Pageable pageable, Criteria criteria) {
        return createQuery(pageable, criteria).all();
    }

    RowsFetchSpec<Historia> createQuery(Pageable pageable, Criteria criteria) {
        List<Expression> columns = HistoriaSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(PacienteSqlHelper.getColumns(rutTable, "rut"));
        SelectFromAndJoinCondition selectFrom = Select
            .builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(rutTable)
            .on(Column.create("rut_id", entityTable))
            .equals(Column.create("id", rutTable));

        String select = entityManager.createSelect(selectFrom, Historia.class, pageable, criteria);
        String alias = entityTable.getReferenceName().getReference();
        String selectWhere = Optional
            .ofNullable(criteria)
            .map(
                crit ->
                    new StringBuilder(select)
                        .append(" ")
                        .append("WHERE")
                        .append(" ")
                        .append(alias)
                        .append(".")
                        .append(crit.toString())
                        .toString()
            )
            .orElse(select); // TODO remove once https://github.com/spring-projects/spring-data-jdbc/issues/907 will be fixed
        return db.sql(selectWhere).map(this::process);
    }

    @Override
    public Flux<Historia> findAll() {
        return findAllBy(null, null);
    }

    @Override
    public Mono<Historia> findById(Long id) {
        return createQuery(null, where("id").is(id)).one();
    }

    private Historia process(Row row, RowMetadata metadata) {
        Historia entity = historiaMapper.apply(row, "e");
        entity.setRut(pacienteMapper.apply(row, "rut"));
        return entity;
    }

    @Override
    public <S extends Historia> Mono<S> insert(S entity) {
        return entityManager.insert(entity);
    }

    @Override
    public <S extends Historia> Mono<S> save(S entity) {
        if (entity.getId() == null) {
            return insert(entity);
        } else {
            return update(entity)
                .map(
                    numberOfUpdates -> {
                        if (numberOfUpdates.intValue() <= 0) {
                            throw new IllegalStateException("Unable to update Historia with id = " + entity.getId());
                        }
                        return entity;
                    }
                );
        }
    }

    @Override
    public Mono<Integer> update(Historia entity) {
        //fixme is this the proper way?
        return r2dbcEntityTemplate.update(entity).thenReturn(1);
    }
}

class HistoriaSqlHelper {

    static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("fecha", table, columnPrefix + "_fecha"));
        columns.add(Column.aliased("diagnostico", table, columnPrefix + "_diagnostico"));
        columns.add(Column.aliased("descripcion", table, columnPrefix + "_descripcion"));
        columns.add(Column.aliased("resultado_file", table, columnPrefix + "_resultado_file"));

        columns.add(Column.aliased("rut_id", table, columnPrefix + "_rut_id"));
        return columns;
    }
}
