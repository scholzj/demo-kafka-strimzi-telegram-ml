FROM registry.access.redhat.com/ubi8/ubi-minimal

RUN microdnf -y install java-11-openjdk-headless openssl && microdnf -y clean all

# Set JAVA_HOME env var
ENV JAVA_HOME /usr/lib/jvm/java

ARG version=latest
ENV VERSION ${version}

ENV DJL_CACHE_DIR /tmp/.djl.ai
ENV ENGINE_CACHE_DIR /tmp/.djl.ai/pytorch

COPY ./scripts/ /bin
COPY ./src/main/resources/log4j2.properties /bin/log4j2.properties

ADD target/kafka-strimzi-telegram-ml-sentiment-analysis-1.0-SNAPSHOT.jar /
ADD target/lib /

CMD ["/bin/run.sh", "SentimentAnalysis"]