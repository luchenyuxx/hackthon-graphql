# -- Build step
FROM ledgerhq/sbt-openjdk-8 as builder

WORKDIR /build
ADD . /build
RUN sbt -Dsbt.ivy.home=.ivy2 -mem 1000 assembly

# -- Run step
FROM ledgerhq/stretch-openjre-8

WORKDIR /app
COPY --from=builder /build/assembly/dsense-gateway*.jar dsense-gateway.jar
COPY --from=builder /build/docker .

# Exposed port must match the one defined in application.conf
EXPOSE 8000
CMD ["/app/run.sh"]
