FROM maven:3.8.6-openjdk-11-slim

WORKDIR /app

COPY . /app

RUN mvn clean package

#CMD ["java", "-jar", "target/data-export-exec.jar"]

#CMD ["./runDataExporter.sh --config_file config.properties --build_jar", "--help"]
CMD ["./runDataExporter.sh", "--config_file", "config.properties", "--build_jar", "--help"]
