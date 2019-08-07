FROM maven:3.6.1-ibmjava-8 as builder
COPY pom.xml /
RUN mvn verify clean --fail-never
COPY src /src
COPY resource /resource
RUN mvn install

FROM ibmjava:8

# install CURL && kubectl and create new user
RUN apt-get update -y \
   && apt-get -y install curl  \
   && useradd -g root -u 1001 kabanero \
   && ARCH=$(uname -p) \
   && if [ "$ARCH" != "ppc64le" ] && [ "$ARCH" != "s390x" ]; then \
     ARCH="amd64" ; \
   fi \
   && curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/${ARCH}/kubectl \ 
   && chmod ug+x ./kubectl \
   && mv ./kubectl /usr/local/bin/kubectl \
   && mkdir /kabanero \
   && chown kabanero:root /kabanero

USER kabanero

WORKDIR /kabanero
COPY --from=builder --chown=kabanero:root target/kabanero-event-1.0-SNAPSHOT-jar-with-dependencies.jar lib/
COPY --from=builder --chown=kabanero:root resource/log4j.info.properties resource/log4j.properties
COPY --from=builder --chown=kabanero:root resource/log4j.info.properties resource/
COPY --from=builder --chown=kabanero:root resource/log4j.debug.properties resource/


ENV LOG4J_CONFIGURATION_FILE=resource/log4j.properties
CMD java -Dcom.ibm.jsse2.overrideDefaultTLS=true -classpath /kabanero/lib/kabanero-event-1.0-SNAPSHOT-jar-with-dependencies.jar io.kabanero.event.Main
