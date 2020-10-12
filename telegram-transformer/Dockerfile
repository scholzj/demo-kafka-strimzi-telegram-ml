FROM registry.access.redhat.com/ubi8/ubi-minimal

RUN microdnf -y install java-11-openjdk-headless openssl && microdnf -y clean all

# Set JAVA_HOME env var
ENV JAVA_HOME /usr/lib/jvm/java

ARG version=latest
ENV VERSION ${version}

COPY ./scripts/ /bin
COPY ./src/main/resources/log4j2.properties /bin/log4j2.properties

ADD target/kafka-strimzi-telegram-ml-transformer-1.0-SNAPSHOT.jar /

CMD ["/bin/run.sh", "/kafka-strimzi-telegram-ml-transformer-1.0-SNAPSHOT.jar"]