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
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.mtbo.lcloud.logging.FileLineLogger;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Cloud discovery using db (postgres) */
public class SqlDiscovery {

  static final FileLineLogger logger = FileLineLogger.getLogger(SqlDiscovery.class.getName());

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
    return createConnection().flatMap(this::create);
  }

  Mono<Boolean> create(R2dbcEntityTemplate template) {
    return template
        .getDatabaseClient()
        .sql(
            """
        create table if not exists instances
        (
            id      uuid not null primary key,
            service varchar(255),
            name    varchar(255),
            last    timestamp with time zone default now(),
            constraint instances_uniq
                unique (name, service)
        );
        """)
        .fetch()
        .rowsUpdated()
        .hasElement()
        .flatMap(
            created ->
                template
                    .getDatabaseClient()
                    .sql("create index if not exists last_index on instances (last)")
                    .fetch()
                    .all()
                    .onErrorComplete()
                    .then(Mono.just(created)))
        .flatMap(
            created ->
                template
                    .getDatabaseClient()
                    .sql(
                        "alter table public.instances alter column id set default gen_random_uuid()")
                    .fetch()
                    .all()
                    .onErrorComplete()
                    .then(Mono.just(created)));
  }

  /**
   * Flux for registering self-instance
   *
   * @param fallback error fallback, can be used for logging. Provide Mono.empty() to retry
   * @return set of successful flux
   */
  public Mono<Boolean> ping(Function<Throwable, Mono<Boolean>> fallback) {
    return createConnection()
        .flatMap(this::update)
        .onErrorResume(fallback)
        .then(Mono.delay(config.updateInterval.dividedBy(2)).then(Mono.just(true)));
  }

  /**
   * Flux for checking instances
   *
   * @param fallback error fallback, can be used for logging. Provide Mono.empty() to retry
   * @return set of instances names flux
   */
  public Flux<Set<String>> lookup(Function<Throwable, Mono<Set<String>>> fallback) {
    return createConnection()
        .flatMap(this::request)
        .onErrorResume(fallback)
        .flatMap(strings -> Mono.delay(config.updateInterval).then(Mono.just(strings)))
        .repeat();
  }

  /**
   * Clean self. Use on service shutdown.
   *
   * @return count of cleaned instances
   */
  public Mono<Long> clean() {
    var threshold = Timestamp.valueOf(LocalDateTime.now().minus(config.updateInterval));

    return createConnection()
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
                    .all());
  }

  /**
   * Clean all out-of-time instances. Use on service start.
   *
   * @param fallback error handler
   * @return count of cleaned instances
   */
  public Mono<Long> cleanAll(Function<Throwable, Mono<Long>> fallback) {
    return createConnection()
        .flatMap(
            template ->
                template
                    .getDatabaseClient()
                    .sql(
                        "delete from instances where service = :service AND EXTRACT (EPOCH from now() - last) > :interval")
                    .bind("service", config.serviceName)
                    .bind("interval", config.updateInterval.toMillis() / 1_000.0)
                    .fetch()
                    .rowsUpdated())
        .onErrorResume(fallback);
  }

  private Mono<Set<String>> request(R2dbcEntityTemplate template) {
    return template
        .getDatabaseClient()
        .sql(
            "select name from instances where service = :service AND EXTRACT (EPOCH from now() - last) < :interval")
        .bind("service", config.serviceName)
        .bind("interval", config.updateInterval.toMillis() / 1_000.0)
        .fetch()
        .all()
        .subscribeOn(Schedulers.boundedElastic())
        .map(stringObjectMap -> (String) stringObjectMap.get("name"))
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
        .flatMap(
            aLong -> {
              if (aLong == 1) {
                if (logger.isLoggable(Level.FINER)) {
                  logger.finer(
                      String.format(
                          "Ping update: %1$d %2$s %3$s",
                          aLong, config.serviceName, config.instanceName));
                }
              }

              return aLong == 1 ? Mono.just(true) : insert(template);
            });
  }

  private Mono<Boolean> insert(R2dbcEntityTemplate template) {
    final var value = UUID.randomUUID();

    if (logger.isLoggable(Level.FINER)) {
      logger.finer(
          String.format(
              "Ping insert: %1$s, %2$s, %3$s", value, config.serviceName, config.instanceName));
    }

    return template
        .getDatabaseClient()
        .sql("INSERT INTO instances (id, service, name) VALUES (:id, :service, :name)")
        .bind("id", value)
        .bind("service", config.serviceName)
        .bind("name", config.instanceName)
        .fetch()
        .rowsUpdated()
        .map(aLong -> aLong == 1);
  }

  private Mono<R2dbcEntityTemplate> createConnection() {
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
