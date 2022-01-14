FROM openjdk:8-jre-alpine

# Prepare for datadir mounts
ENV DOCKER true

# Copy the application from its folder to ours
COPY ./build/install/bootstrap-downloader /bootstrap-downloader

WORKDIR /bootstrap-downloader/bin

# Run the app when the container is executed.
ENTRYPOINT ["./bootstrap-downloader"]
