Subject: Options for Calling ALS GetRefDt from ENTITY-SERVICE

Hi Samuel,

I've been looking into how we can call the GetRefDt function from the ALS schema into our ENTITY-SERVICE. Here are the three options I see:

1. DB Link / Schema Grant – If both schemas are on the same DB instance, we can just grant execute on ALS.GetRefDt to the ENTITY user and call it directly with a schema-qualified native query. If they're on different instances, we'd set up a DB link. Simplest approach, but creates tighter coupling at the database level.

2. REST Web Service – Expose GetRefDt as a REST endpoint from an ALS-SERVICE, then consume it from ENTITY-SERVICE using WebClient (same pattern we already use with EtlServiceClient). Cleanest from a microservices standpoint, but adds network overhead.

3. Second DataSource – Configure a second DataSource in ENTITY-SERVICE pointing to the ALS schema and call the function directly via a separate JdbcTemplate. No DB link or new service needed, but ENTITY-SERVICE would need ALS credentials.

Let me know which direction you'd prefer and we can move forward with the implementation.

Thanks


Great question. Given your architecture (Spring Boot services with separate database schemas), here are three solid options for calling the `GetRefDt` function from the ALS schema into your ENTITY-SERVICE:

---

**Option 1: Oracle Database Link (DB Link)**

This is the most straightforward database-level approach. You create a DB link from the ENTITY schema to the ALS schema and call the function directly.

```sql
-- DBA or schema owner creates the link
CREATE DATABASE LINK als_link
  CONNECT TO als_user IDENTIFIED BY als_password
  USING 'ALS_DB_TNS';

-- Then in your ENTITY-SERVICE repository/native query:
SELECT ALS.GetRefDt@als_link(:param1) FROM DUAL;
```

If both schemas are on the **same database instance**, you don't even need a DB link — just grant execute privilege:

```sql
-- Run as ALS schema owner or DBA
GRANT EXECUTE ON ALS.GetRefDt TO ENTITY_USER;

-- Then call it directly with schema prefix
SELECT ALS.GetRefDt(:param1) FROM DUAL;
```

In your Java service, you'd use a `@Query` with `nativeQuery = true` or a `JdbcTemplate`:

```java
@Repository
public class AlsFunctionRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Date getRefDt(String param) {
        Query query = entityManager.createNativeQuery(
            "SELECT ALS.GetRefDt(:param) FROM DUAL");
        query.setParameter("param", param);
        return (Date) query.getSingleResult();
    }
}
```

**Pros:** Simple, low latency, transactionally consistent.
**Cons:** Tight coupling between schemas, DB links need DBA involvement, harder to manage across environments.

---

**Option 2: Expose as a REST Web Service (Recommended for Microservices)**

This is the cleanest option if you're following microservices patterns — which your project already does with the `EtlServiceClient` using `WebClient`. You'd create (or use an existing) ALS-SERVICE that exposes `GetRefDt` as a REST endpoint, and then call it from ENTITY-SERVICE.

**ALS-SERVICE side** (new or existing controller):

```java
@RestController
@RequestMapping("/api/als")
public class AlsController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/ref-date")
    public ResponseEntity<LocalDate> getRefDt(@RequestParam String param) {
        LocalDate result = jdbcTemplate.queryForObject(
            "SELECT GetRefDt(?) FROM DUAL",
            LocalDate.class, param);
        return ResponseEntity.ok(result);
    }
}
```

**ENTITY-SERVICE side** (consuming it via WebClient, similar to your existing `EtlServiceClient`):

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AlsServiceClient {

    private final WebClient alsServiceWebClient;

    public Mono<LocalDate> getRefDt(String param) {
        return alsServiceWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/als/ref-date")
                    .queryParam("param", param)
                    .build())
                .retrieve()
                .bodyToMono(LocalDate.class)
                .timeout(Duration.ofSeconds(10))
                .doOnError(e -> log.error("Error calling ALS GetRefDt: {}", e.getMessage()));
    }
}
```

**Pros:** Loose coupling, schema independence, fits your existing architecture, independently deployable/scalable.
**Cons:** Network overhead, need to handle failures/retries/circuit-breaking, not transactionally consistent.

---

**Option 3: Second DataSource in ENTITY-SERVICE**

Configure ENTITY-SERVICE with a second `DataSource` pointing directly to the ALS schema. No DB link needed, no separate web service needed.

```yaml
# application.yml
spring:
  datasource:
    entity:
      url: jdbc:oracle:thin:@entity-db:1521/ENTITYDB
      username: entity_user
      password: entity_pass
    als:
      url: jdbc:oracle:thin:@als-db:1521/ALSDB
      username: als_user
      password: als_pass
```

```java
@Configuration
public class AlsDataSourceConfig {

    @Bean("alsDataSource")
    @ConfigurationProperties("spring.datasource.als")
    public DataSource alsDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean("alsJdbcTemplate")
    public JdbcTemplate alsJdbcTemplate(@Qualifier("alsDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
```

```java
@Service
public class AlsFunctionService {

    @Autowired
    @Qualifier("alsJdbcTemplate")
    private JdbcTemplate alsJdbcTemplate;

    public LocalDate getRefDt(String param) {
        return alsJdbcTemplate.queryForObject(
            "SELECT GetRefDt(?) FROM DUAL", LocalDate.class, param);
    }
}
```

**Pros:** No DB link, no separate service to maintain, direct and fast.
**Cons:** ENTITY-SERVICE now needs ALS credentials, tighter coupling than the web service approach, two connection pools to manage.

---

# Option 4: GraphQL

We could also expose `GetRefDt` via a GraphQL endpoint on the ALS-SERVICE side. ENTITY-SERVICE would then query it using Spring's `HttpGraphQlClient`.

## Why Consider It

- **Single request, multiple fields** — If we ever need to call more than just `GetRefDt` from ALS (e.g., `getRefStatus`, `getRefConfig`), GraphQL lets us batch those into one request instead of multiple REST calls.
- **No over-fetching** — The client asks for exactly what it needs.
- **Strongly typed schema** — The `.graphqls` schema file acts as a contract between services.

## When It's Overkill

If we're only ever calling `GetRefDt` and nothing else, a simple REST endpoint is easier to set up and maintain. GraphQL adds schema definitions, resolvers, and a new dependency (`spring-boot-starter-graphql`) that may not be justified for a single function call.

## Recommendation

Start with REST (Option 2) for now. If our cross-schema queries grow in number or complexity, we can migrate to GraphQL at that point without major rework.

**Which to choose?**

If both schemas are on the same DB instance, the **grant + schema-qualified call** (Option 1 without a DB link) is the simplest. If you want architectural cleanliness and already have the service-to-service communication pattern in place (which you do with `EtlServiceClient`), **Option 2 (REST)** is the most consistent with your existing design. Option 3 is a good middle ground if standing up a new ALS-SERVICE feels like overkill.
