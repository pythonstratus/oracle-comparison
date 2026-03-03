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


Subject: GetRefDt in ENTITY-SERVICE – ALS Dependency Options & Recommendations

Hi Samuel,

I've been looking into the GetRefDt function in ENTITYDEV and how we can handle its dependencies on the ALS schema. As it stands, the function directly queries ALS tables (ALS.XE, ALS.LS, ALS.LD, ALS.LA), so we need to decide how to manage that cross-schema access. Here are four options:

1. DB Link / Schema Grant (Database Level)
If both schemas are on the same DB instance, we grant SELECT on the required ALS tables (XE, LS, LD, LA) and EXECUTE on any ALS functions to the ENTITY user, then reference them with the schema prefix (ALS.XE, etc.) — which is what the function already does. If the schemas are on different instances, we'd need a DB link.

Pros:
- Simplest to implement — the function already references ALS tables directly
- Low latency, runs in a single DB round-trip
- Transactionally consistent

Cons:
- Tight coupling between ENTITY and ALS schemas
- Requires DBA involvement for grants/links
- Schema changes in ALS can silently break ENTITYDEV.GetRefDt
- Harder to manage across environments (dev, test, prod)

2. REST Web Service
Create a REST endpoint on an ALS-SERVICE that wraps the GetRefDt logic. ENTITY-SERVICE calls it via WebClient, same pattern we use with EtlServiceClient.

Pros:
- Loose coupling — ENTITY-SERVICE has no direct dependency on ALS tables
- Fits our existing microservices architecture
- ALS team owns and maintains the logic
- Independently deployable and scalable

Cons:
- Network overhead and latency
- Need to handle failures, retries, and circuit-breaking
- Not transactionally consistent with ENTITY-SERVICE operations
- Requires ALS team to build and maintain the endpoint

3. Second DataSource in ENTITY-SERVICE
Configure ENTITY-SERVICE with a second DataSource pointing to the ALS schema. The function queries ALS tables directly through the second connection pool — no DB link or separate service needed.

Pros:
- No DB link or new service to maintain
- Direct and fast queries
- All logic stays within ENTITY-SERVICE

Cons:
- ENTITY-SERVICE needs ALS credentials and connection config
- Two connection pools to manage
- Still tightly coupled to ALS table structures
- Credential management complexity across environments

4. GraphQL
Expose ALS data through a GraphQL endpoint on the ALS-SERVICE side. ENTITY-SERVICE queries it using Spring's HttpGraphQlClient.

Pros:
- Flexible querying — can batch multiple ALS data needs in a single request
- No over-fetching, client requests exactly what it needs
- Strongly typed schema acts as a contract
- Good fit if we anticipate more cross-schema queries in the future

Cons:
- Adds complexity (schema definitions, resolvers, new dependency)
- Overkill if GetRefDt is the only cross-schema call
- Same network/latency concerns as REST
- Team needs to be comfortable with GraphQL

My recommendation: Given that GetRefDt already directly queries four ALS tables, the quickest path is Option 1 (grants are likely already in place since the function exists in ENTITYDEV). However, if we want to decouple long-term, Option 2 (REST) aligns best with our current architecture. Option 4 (GraphQL) becomes worth it if we foresee more cross-schema query needs.

Let me know which direction you'd prefer and we can move forward.

Thanks
