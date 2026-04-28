SERVER_IMAGE := algosys/server

.PHONY: install server client build clean

install:
	docker build -t $(SERVER_IMAGE) ./server
	cd client && mvn dependency:resolve

server: install
	docker ps -q --filter ancestor=$(SERVER_IMAGE) | xargs -r docker stop
	docker run --rm -p 5000:5000 $(SERVER_IMAGE)

client:
	cd client && mvn javafx:run

build:
	cd client && mvn package

clean:
	cd client && mvn clean
	find server -name "__pycache__" -exec rm -rf {} + 2>/dev/null; true
