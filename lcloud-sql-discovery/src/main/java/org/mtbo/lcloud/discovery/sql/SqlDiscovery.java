/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.sql;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import io.r2dbc.spi.ConnectionFactories;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Cloud discovery using db (postgres) */
public class SqlDiscovery {

  // "r2dbc:postgresql://user:user@localhost:5444/demo"
  final Config config;

  /**
   * Constructor with parameters
   *
   * @param config configuration
   */
  public SqlDiscovery(Config config) {
    this.config = config;
  }

  /**
   * Creates instances table if not exists
   *
   * @return True in case of OK
   */
  public Mono<Boolean> initialize() {
    return createTemplate().flatMap(this::create);
  }

  Mono<Boolean> create(R2dbcEntityTemplate template) {
    return template
        .getDatabaseClient()
        .sql(
            """
create table if not exists instances
(
    id      uuid not null
        primary key,
    service varchar(255),
    name    varchar(255),
    last    timestamp with time zone default now(),
    constraint instances_uniq
        unique (name, service)
);
create index if not exists last_index
    on instances (last);
                        """)
        .fetch()
        .rowsUpdated()
        .hasElement();
  }

  /**
   * Flux for registering self-instance
   *
   * @param fallback error fallback, can be used for logging. Provide Mono.empty() to retry
   * @return set of successful flux
   */
  public Flux<Boolean> ping(Function<Throwable, Mono<Boolean>> fallback) {
    return createTemplate()
        .repeat()
        .concatMap(
            template ->
                Flux.from(
                    Mono.delay(config.updateInterval.dividedBy(2))
                        .flatMap(aLong -> update(template).onErrorResume(fallback))));
  }

  /**
   * Flux for checking instances
   *
   * @param fallback error fallback, can be used for logging. Provide Mono.empty() to retry
   * @return set of instances names flux
   */
  public Flux<Set<String>> lookup(Function<Throwable, Mono<Set<String>>> fallback) {
    return Flux.from(
        createTemplate()
            .repeat()
            .concatMap(
                template ->
                    Mono.delay(config.updateInterval)
                        .flatMap(aLong -> request(template).onErrorResume(fallback))));
  }

  /**
   * Clean self. Use on service shutdown.
   *
   * @param fallback error handler
   * @return count of cleaned instances
   */
  public Mono<Long> clean(Function<Throwable, Mono<Boolean>> fallback) {
    var threshold = Timestamp.valueOf(LocalDateTime.now().minus(config.updateInterval));

    return createTemplate()
        .flatMap(
            template ->
                template
                    .delete(Instances.class)
                    .matching(
                        query(
                            where("service")
                                .is(config.serviceName)
                                .and("name")
                                .is(config.instanceName)
                                .and("last")
                                .lessThan(threshold)))
                    .all())
        .onErrorComplete();
  }

  /**
   * Clean all out-of-time instances. Use on service start.
   *
   * @param fallback error handler
   * @return count of cleaned instances
   */
  public Mono<Long> cleanAll(Function<Throwable, Mono<Boolean>> fallback) {
    var threshold = Timestamp.valueOf(LocalDateTime.now().minus(config.updateInterval));

    return createTemplate()
        .flatMap(
            template ->
                template
                    .delete(Instances.class)
                    .matching(
                        query(
                            where("service")
                                .is(config.serviceName)
                                .and("last")
                                .lessThan(threshold)))
                    .all())
        .onErrorComplete();
  }

  private Mono<Set<String>> request(R2dbcEntityTemplate template) {
    var threshold = Timestamp.valueOf(LocalDateTime.now().minus(config.updateInterval));
    return template
        .select(Instances.class)
        .matching(query(where("service").is(config.serviceName).and("last").greaterThan(threshold)))
        .all()
        .map(Instances::name)
        .collect(Collectors.toSet());
  }

  private Mono<Boolean> update(R2dbcEntityTemplate template) {
    return template
        .getDatabaseClient()
        .sql("update instances set last = now() where service=:service and name=:name")
        .bind("service", config.serviceName)
        .bind("name", config.instanceName)
        .fetch()
        .rowsUpdated()
        .flatMap(aLong -> aLong == 1 ? Mono.just(true) : insert(template));
  }

  private Mono<Boolean> insert(R2dbcEntityTemplate template) {
    return template
        .getDatabaseClient()
        .sql("INSERT INTO instances (id, service, name) VALUES (:id, :service, :name)")
        .bind("id", UUID.randomUUID())
        .bind("service", config.serviceName)
        .bind("name", config.instanceName)
        .fetch()
        .rowsUpdated()
        .map(aLong -> aLong == 1);
  }

  private Mono<R2dbcEntityTemplate> createTemplate() {
    var connectionFactory = ConnectionFactories.get(config.connectionString);

    var entityTemplate = new R2dbcEntityTemplate(connectionFactory);

    return Mono.just(entityTemplate).publishOn(Schedulers.boundedElastic()).cache();
  }

  /**
   * sql discovery configuration
   *
   * @param connectionString db connection string
   * @param serviceName service name
   * @param instanceName instance name
   * @param updateInterval update interval
   */
  public record Config(
      String connectionString, String serviceName, String instanceName, Duration updateInterval) {}
}
