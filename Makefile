docker-pull:
	docker pull maven:3.8.6-openjdk-11-slim

.PHONY: build-image
build-image: docker-pull \
             $(call print-help,build, "Build the docker image.")
	docker build -t reactome/release-data-exporter:latest .

.PHONY: run-image
run-image: $(call print-help,run, "Run the docker image.")
	docker run reactome/release-data-exporter:latest -v $(pwd)/output:/graphdb --net=host
