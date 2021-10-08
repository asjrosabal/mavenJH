package com.mycompany.myapp.repository;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import com.mycompany.myapp.domain.Paciente;
import com.mycompany.myapp.repository.rowmapper.EspecialistaRowMapper;
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
 * Spring Data SQL reactive custom repository implementation for the Paciente entity.
 */
@SuppressWarnings("unused")
class PacienteRepositoryInternalImpl implements PacienteRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final EspecialistaRowMapper especialistaMapper;
    private final PacienteRowMapper pacienteMapper;

    private static final Table entityTable = Table.aliased("paciente", EntityManager.ENTITY_ALIAS);
    private static final Table rutTable = Table.aliased("especialista", "rut");

    public PacienteRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        EspecialistaRowMapper especialistaMapper,
        PacienteRowMapper pacienteMapper
    ) {
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.especialistaMapper = especialistaMapper;
        this.pacienteMapper = pacienteMapper;
    }

    @Override
    public Flux<Paciente> findAllBy(Pageable pageable) {
        return findAllBy(pageable, null);
    }

    @Override
    public Flux<Paciente> findAllBy(Pageable pageable, Criteria criteria) {
        return createQuery(pageable, criteria).all();
    }

    RowsFetchSpec<Paciente> createQuery(Pageable pageable, Criteria criteria) {
        List<Expression> columns = PacienteSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(EspecialistaSqlHelper.getColumns(rutTable, "rut"));
        SelectFromAndJoinCondition selectFrom = Select
            .builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(rutTable)
            .on(Column.create("rut_id", entityTable))
            .equals(Column.create("id", rutTable));

        String select = entityManager.createSelect(selectFrom, Paciente.class, pageable, criteria);
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
    public Flux<Paciente> findAll() {
        return findAllBy(null, null);
    }

    @Override
    public Mono<Paciente> findById(Long id) {
        return createQuery(null, where("id").is(id)).one();
    }

    private Paciente process(Row row, RowMetadata metadata) {
        Paciente entity = pacienteMapper.apply(row, "e");
        entity.setRut(especialistaMapper.apply(row, "rut"));
        return entity;
    }

    @Override
    public <S extends Paciente> Mono<S> insert(S entity) {
        return entityManager.insert(entity);
    }

    @Override
    public <S extends Paciente> Mono<S> save(S entity) {
        if (entity.getId() == null) {
            return insert(entity);
        } else {
            return update(entity)
                .map(
                    numberOfUpdates -> {
                        if (numberOfUpdates.intValue() <= 0) {
                            throw new IllegalStateException("Unable to update Paciente with id = " + entity.getId());
                        }
                        return entity;
                    }
                );
        }
    }

    @Override
    public Mono<Integer> update(Paciente entity) {
        //fixme is this the proper way?
        return r2dbcEntityTemplate.update(entity).thenReturn(1);
    }
}

class PacienteSqlHelper {

    static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("nombre", table, columnPrefix + "_nombre"));
        columns.add(Column.aliased("apellidos", table, columnPrefix + "_apellidos"));
        columns.add(Column.aliased("rut", table, columnPrefix + "_rut"));
        columns.add(Column.aliased("fecha_nacimiento", table, columnPrefix + "_fecha_nacimiento"));

        columns.add(Column.aliased("rut_id", table, columnPrefix + "_rut_id"));
        return columns;
    }
}
