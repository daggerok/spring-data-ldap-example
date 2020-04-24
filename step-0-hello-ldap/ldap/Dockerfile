FROM osixia/openldap-backup:1.3.0
LABEL MAINTAINER="Maksim Kostromin <daggerok@gmail.com> https://githuib.com/daggerok/spring-data-ldap-example"
ENTRYPOINT ["/bin/bash"]
CMD ["-c", "/container/tool/run --copy-service -l debug"]
COPY --chown=openldap:openldap ./test-data.ldif /container/service/slapd/assets/config/bootstrap/ldif/50-test-data.ldif
