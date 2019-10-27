FROM openjdk:8-jre-alpine

WORKDIR /nodecore-pop/bin

COPY ./build/install/nodecore-pop /nodecore-pop

ENTRYPOINT ["./nodecore-pop", "-d", "/data", "-c", "/data/ncpop.properties"]
