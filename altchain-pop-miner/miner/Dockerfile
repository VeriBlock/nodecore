FROM openjdk:8

# Prepare for datadir mounts
ENV DATA_DIR /data
ENV CONFIG_FILE /data/application.conf

EXPOSE 4567

# Copy the application from its folder to ours
COPY ./build/install/veriblock-alt-pop /veriblock-alt-pop
# Create the data directory
RUN mkdir /data

WORKDIR /veriblock-alt-pop/bin

# Run the app when the container is executed.
ENTRYPOINT ["./veriblock-alt-pop"]
