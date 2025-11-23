# syntax=docker/dockerfile:1.6

###############################################
# Etapa de compilacion                       #
# Usa UBI OpenJDK 21 + Maven y permite       #
# reutilizar el repositorio ~/.m2 del host.  #
###############################################
ARG BASE_IMAGE=default-route-openshift-image-registry.apps-crc.testing/redeban-base-image/java21-runtime:1.0.0
##ARG BASE_IMAGE=registry.access.redhat.com/ubi9/openjdk-21-runtime

FROM ${BASE_IMAGE}

WORKDIR /work
COPY target/quarkus-app/lib/     ./lib/
COPY target/quarkus-app/app/     ./app/
COPY target/quarkus-app/quarkus/ ./quarkus/
COPY target/quarkus-app/*.jar    ./

EXPOSE 8080
USER 1001

# ENTRYPOINT heredado de la imagen base
