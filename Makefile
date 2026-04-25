VENV := server/.venv
PYTHON := $(VENV)/bin/python

.PHONY: install server client build clean

install:
	python3 -m venv $(VENV)
	$(VENV)/bin/pip install -r server/requirements.txt
	cd client && mvn dependency:resolve

server:
	$(PYTHON) server/app.py

client:
	cd client && mvn javafx:run

build:
	cd client && mvn package

clean:
	cd client && mvn clean
	find server -name "__pycache__" -exec rm -rf {} + 2>/dev/null; true
