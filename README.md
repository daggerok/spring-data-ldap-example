# Spring + LDAP [![CI](https://github.com/daggerok/spring-data-ldap-example/workflows/CI/badge.svg)](https://github.com/daggerok/spring-data-ldap-example/actions?query=workflow%3ACI)
Play with LDAP in Docker using Spring java APIs...

## LDAP basics (docker)

defaults

```bash
# run LDAP server (dy default: Example Inc. with example.org domain):
docker run -p 389:389 -p 636:636 --rm -it --name ldap osixia/openldap:1.3.0

# exec LDAP query:
docker exec ldap ldapsearch -x -H ldap://localhost:389 -b dc=example,dc=org -D "cn=admin,dc=example,dc=org" -w admin
```

customize

```bash
docker run -p 389:389 -p 636:636 --rm -it --name ldap \
  --env LDAP_ORGANISATION="My Test Company Inc." \
  --env LDAP_DOMAIN="my-test-company-domain.com" \
  osixia/openldap:1.3.0

# exec query
docker exec ldap ldapsearch -x -H ldap://localhost:389 -b dc=my-test-company-domain,dc=com -D "cn=admin,dc=my-test-company-domain,dc=com" -w admin
```

## step: 0

let's prepare _LDAP_ in _Docker_ by using [osixia](https://github.com/osixia/docker-openldap/) solution.

create `step-0-hello-ldap/ldap/Dockerfile` file:

```Dockerfile
FROM osixia/openldap-backup:1.3.0
LABEL MAINTAINER="Maksim Kostromin <daggerok@gmail.com> https://githuib.com/daggerok/spring-data-ldap-example"
ENTRYPOINT ["/bin/bash"]
CMD ["-c", "/container/tool/run --copy-service -l debug"]
COPY --chown=openldap:openldap ./test-data.ldif /container/service/slapd/assets/config/bootstrap/ldif/50-test-data.ldif
```

create `step-0-hello-ldap/ldap/test-data.ldif` file:

```
version: 1

# Entry: uid=user,dc=my-test-company-domain,dc=com
# user: uid=user,dc=my-test-company-domain,dc=com
# password: password
dn: uid=user,dc=my-test-company-domain,dc=com
uid: user
cn: user
sn: 3
description: My Test Company LDAP user organization account
objectclass: top
objectClass: inetOrgPerson
mail: user@my-test-company-domain.com
userPassword: password

# Entries already exists / provided by docker container:
# Entry 1: dc=my-test-company-domain,dc=com
# Entry 2: cn=admin,dc=my-test-company-domain,dc=com
# Admin user: cn=admin,dc=my-test-company-domain,dc=com
# Admin password: adm1nZupperUberP@assw0rd!!1111oneoneone
```

create `step-0-hello-ldap/docker-compose.yaml` file:

```yaml
version: '2.1'
networks:
  my-test-company-domain.com:
services:
  ldap:
    hostname: ldap.my-test-company-domain.com
    build: ./ldap
    environment:
      LDAP_ORGANISATION: My Test Company Inc.
      LDAP_DOMAIN: my-test-company-domain.com
      LDAP_ADMIN_PASSWORD: adm1nZupperUberP@assw0rd!!1111oneoneone
      LDAP_BACKUP_CONFIG_CRON_EXP: '* * * * *'
      LDAP_BACKUP_DATA_CRON_EXP: '*/15 * * * *'
      LDAP_BACKUP_TTL: 7
    ports:
    - '389:389'
    - '636:636'
    networks:
      my-test-company-domain.com:
        aliases:
          - ldap
          - ldap.my-test-company-domain.com
    healthcheck:
      test: ( ( test 1 -eq `ss -tulwn | grep '0.0.0.0:389' | wc -l` ) && ( test 1 -eq `ss -tulwn | grep '0.0.0.0:636' | wc -l` ) ) || exit 1
      interval: 5s
      timeout: 5s
      retries: 55
  # omit ldap-admin-uiand step-0-hello-ldap definitions...
```

add _dependencies_ in `step-0-hello-ldap/pom.xml` file:

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-ldap</artifactId>
  </dependency>
</dependencies>
```

add _configurations_ in `step-0-hello-ldap/src/main/resources/application.properties` file:

```properties
spring.ldap.urls=ldap://${LDAP_HOST:127.0.0.1}:${LDAP_PORT:389}
spring.ldap.username=${LDAP_USER:cn=admin,dc=my-test-company-domain,dc=com}
spring.ldap.password=${LDAP_PASSWORD:adm1nZupperUberP@assw0rd!!1111oneoneone}
spring.ldap.base=dc=my-test-company-domain,dc=com
```

implement _java_ app:

```java
@Log4j2
@RestController
@RequiredArgsConstructor
class LdapResource {

  final LdapTemplate ldapTemplate;

  @RequestMapping("/")
  ResponseEntity<?> index(@RequestBody(required = false) Optional<LinkedHashMap<String, String>> request) {
    var query = request.map(map -> map.get("query"))
                       .orElse("objectClass=inetOrgPerson");

    if (!query.contains("=")) return ResponseEntity
        .badRequest().body(Collections.singletonMap("error", "Invalid query. Use key=value format!"));

    String[] kv = query.split("=");
    var searchResults = ldapTemplate.search(
        LdapQueryBuilder.query().where(kv[0]).is(kv[1]),
        (AttributesMapper<Collection<String>>) attributes -> {
          Iterator<? extends Attribute> iterator = attributes.getAll().asIterator();
          return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                              .map(String::valueOf)
                              .collect(Collectors.toList());
        }
    );

    return ResponseEntity.ok()
                         .body(searchResults.stream()
                                            .flatMap(Collection::stream)
                                            .collect(Collectors.toList()));
  }
}
```

simplify _testing_ for all boiler-plates with docker-compose:

```bash
./mvnw -f step-0-hello-ldap clean package spring-boot:build-image docker-compose:up
http :8080
http :8080 query=objectClass=top
./mvnw -f step-0-hello-ldap docker-compose:down
```

## resources
* [phpLdapAdmin](https://github.com/osixia/docker-phpLDAPadmin)
* [LDAP Env](https://github.com/osixia/docker-openldap#defaultstartupyaml)
* [LDAP in Docker](https://github.com/osixia/docker-openldap/)
* [Spring LDAP](https://docs.spring.io/spring-boot/docs/2.2.6.RELEASE/reference/htmlsingle/#boot-features-ldap)
* [Old Spring LDAP references](https://docs.spring.io/spring-ldap/docs/current/reference/)
<!--
* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.2.6.RELEASE/maven-plugin/)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.2.6.RELEASE/reference/htmlsingle/#boot-features-developing-web-applications)
* [Spring Configuration Processor](https://docs.spring.io/spring-boot/docs/2.2.6.RELEASE/reference/htmlsingle/#configuration-metadata-annotation-processor)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/bookmarks/)
-->
