package playwithspringldap;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.directory.Attribute;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
        // (AttributesMapper<Collection<Map>>) attributes -> {
        //   Iterator<? extends Attribute> iterator = attributes.getAll().asIterator();
        //   return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
        //                       .map(String::valueOf)
        //                       .map(s -> {
        //                         var strings = s.split(":");
        //                         if (strings.length < 2) throw new RuntimeException("invalid data....");
        //                         return strings.length == 2 ? Collections.singletonMap(strings[0], strings[1])
        //                             : Collections.singletonMap(strings[0], Arrays.stream(strings).skip(1).collect(Collectors.toList()));
        //                       })
        //                       // .collect(Collectors.joining("\n"));
        //                       .collect(Collectors.toList());
        // }
        (AttributesMapper<Collection<String>>) attributes -> {
          Iterator<? extends Attribute> iterator = attributes.getAll().asIterator();
          return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                              .map(String::valueOf)
                              .collect(Collectors.toList());
        }
    );
    // // because of possible: Duplicate key objectClass...
    // return ResponseEntity.ok().body(searchResults.stream().flatMap(Collection::stream).map(s -> s.split(": ")).collect(Collectors.toMap(
    //     strings -> strings[0],
    //     strings -> Arrays.stream(strings).skip(1).collect(Collectors.joining())
    // )));
    return ResponseEntity.ok()
                         .body(searchResults.stream()
                                            .flatMap(Collection::stream)
                                            .collect(Collectors.toList()));
  }
}

@SpringBootApplication
public class PlayWithSpringLdapApplication {

  public static void main(String[] args) {
    SpringApplication.run(PlayWithSpringLdapApplication.class, args);
  }
}
