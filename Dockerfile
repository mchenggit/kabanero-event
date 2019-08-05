FROM maven:3.6.1-ibmjava-8 as builder
COPY src /src
COPY pom.xml /
COPY adminclient /adminclient
COPY resource /resource
RUN mvn install

FROM ibmjava:8-jre-ubi-min

LABEL name="IBM Application Navigator" \
      vendor="IBM" \
      version="1.0.0" \
      release="1.0.0" \
      summary="WAS Controller image for IBM Application Navigator" \
      description="This image contains the WAS Controller for IBM Application Navigator"

# install CURL && kubectl and create new user
RUN microdnf update -y \
   && microdnf install shadow-utils \
   && adduser -u 1001 -r -g root -s /usr/sbin/nologin wascontroller \
   && ARCH=$(uname -p) \
   && if [ "$ARCH" != "ppc64le" ] && [ "$ARCH" != "s390x" ]; then \
     ARCH="amd64" ; \
   fi \
   && curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/${ARCH}/kubectl \ 
   && chmod ug+x ./kubectl \
   && mv ./kubectl /usr/local/bin/kubectl \
   && mkdir /WASController \
   && chown wascontroller:root WASController \
   && microdnf remove -y shadow-utils \
   && microdnf clean all

USER wascontroller

COPY licenses/ /licenses/

WORKDIR /WASController
COPY --from=builder --chown=wascontroller:root target/tWASController-1.0-SNAPSHOT-jar-with-dependencies.jar lib/
COPY --from=builder --chown=wascontroller:root adminclient/com.ibm.ws.admin.client_9.0.jar lib/
COPY --from=builder --chown=wascontroller:root adminclient/soap.client.props adminclient/
COPY --from=builder --chown=wascontroller:root resource/log4j.info.properties resource/log4j.properties
COPY --from=builder --chown=wascontroller:root resource/log4j.info.properties resource/
COPY --from=builder --chown=wascontroller:root resource/log4j.debug.properties resource/

COPY --chown=1001:0 testcntlr.sh /bin/testcntlr.sh

ENV WAS_HOME /WASController
ENV USER_INSTALL_ROOT ${WAS_HOME}
ENV SOAP_PROPS -Dcom.ibm.SOAP.ConfigURL=${WAS_HOME}/adminclient/soap.client.props
ENV LOG4J_CONFIGURATION_FILE=resource/log4j.properties
CMD java -classpath ${WAS_HOME}/lib/tWASController-1.0-SNAPSHOT-jar-with-dependencies.jar:${WAS_HOME}/lib/com.ibm.ws.admin.client_9.0.jar ${SOAP_PROPS} com.ibm.ws.prism.controller.WASController
