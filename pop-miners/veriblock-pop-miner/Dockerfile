FROM openjdk:8-jre-alpine

WORKDIR /veriblock-pop-miner/bin

COPY ./build/install/veriblock-pop-miner /veriblock-pop-miner

ENTRYPOINT ["./veriblock-pop-miner", "-d", "/data", "-c", "/data/application.conf"]
