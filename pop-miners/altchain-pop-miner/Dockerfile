FROM openjdk:8-jre-alpine

# Prepare for datadir mounts
ENV DATA_DIR /data
ENV CONFIG_FILE $DATA_DIR/application.conf
ENV APM_LOG_PATH $DATA_DIR/logs
ENV DOCKER true

EXPOSE 4567

# Copy the application from its folder to ours
COPY ./build/install/altchain-pop-miner /altchain-pop-miner
# Create the data directory
RUN mkdir /data

WORKDIR /altchain-pop-miner/bin

# Run the app when the container is executed.
ENTRYPOINT ["./altchain-pop-miner"]
