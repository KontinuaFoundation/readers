FROM python:3.12-slim-bookworm

WORKDIR /app

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt
RUN pip install --no-cache-dir gunicorn

COPY . .

EXPOSE 8000

ENTRYPOINT ["/bin/sh", "-c"]
CMD ["exec gunicorn readers_backend.wsgi:application --bind 0.0.0.0:8000 --worker-class gthread --workers ${GUNICORN_WORKERS:-1} --threads ${GUNICORN_THREADS:-2}"]