FROM openjdk:8-jre-alpine

# Prepare for datadir mounts
ENV DATA_DIR /data
ENV CONFIG_FILE $DATA_DIR/application.conf
ENV SPV_LOG_PATH $DATA_DIR/logs
ENV DOCKER true

EXPOSE 4567

ENV DOCKER true

# Copy the application from its folder to ours
COPY ./build/install/veriblock-spv /veriblock-spv
# Create the data directory
RUN mkdir /data

WORKDIR /veriblock-spv/bin

# Run the app when the container is executed.
ENTRYPOINT ["./veriblock-spv"]
