FROM postgres:16

# Optional environment variables (you can override at runtime too)
ENV POSTGRES_USER=appuser
ENV POSTGRES_PASSWORD=apppassword
ENV POSTGRES_DB=appdb

# Optional: initialize scripts (runs on first startup only)
# COPY ./init.sql /docker-entrypoint-initdb.d/

# Expose PostgreSQL port
EXPOSE 5432

# The base image already defines the correct ENTRYPOINT and CMD