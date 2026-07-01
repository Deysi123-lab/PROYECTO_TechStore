FROM python:3.12-slim

WORKDIR /docs

RUN pip install --no-cache-dir mkdocs mkdocs-material pymdown-extensions

COPY mkdocs.yml .
COPY docs ./docs

CMD ["mkdocs", "build"]
