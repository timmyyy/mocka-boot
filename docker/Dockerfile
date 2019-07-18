FROM openjdk:8-jre-alpine
ARG JAVA_OPTS

ADD mock-boot-0.0.1.jar app.jar
EXPOSE 8081
ENTRYPOINT java $JAVA_OPTS -jar app.jar