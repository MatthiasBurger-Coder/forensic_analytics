#!/usr/bin/env bash
set -Eeuo pipefail

: "${POSTGRES_DB:?POSTGRES_DB must be set}"
: "${POSTGRES_USER:?POSTGRES_USER must be set}"
: "${REPOSITORY_SOURCE_DB_USER:?REPOSITORY_SOURCE_DB_USER must be set}"
: "${REPOSITORY_SOURCE_DB_PASSWORD:?REPOSITORY_SOURCE_DB_PASSWORD must be set}"
: "${REPOSITORY_SOURCE_DB_SCHEMA:?REPOSITORY_SOURCE_DB_SCHEMA must be set}"

psql \
  --username "${POSTGRES_USER}" \
  --dbname "${POSTGRES_DB}" \
  --set=ON_ERROR_STOP=1 \
  --set=db_name="${POSTGRES_DB}" \
  --set=repo_user="${REPOSITORY_SOURCE_DB_USER}" \
  --set=repo_password="${REPOSITORY_SOURCE_DB_PASSWORD}" \
  --set=repo_schema="${REPOSITORY_SOURCE_DB_SCHEMA}" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'repo_user', :'repo_password')
WHERE NOT EXISTS (
  SELECT 1
  FROM pg_roles
  WHERE rolname = :'repo_user'
)\gexec

SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'repo_user', :'repo_password')\gexec

GRANT CONNECT ON DATABASE :"db_name" TO :"repo_user";
CREATE SCHEMA IF NOT EXISTS :"repo_schema" AUTHORIZATION :"repo_user";
ALTER SCHEMA :"repo_schema" OWNER TO :"repo_user";
GRANT USAGE, CREATE ON SCHEMA :"repo_schema" TO :"repo_user";
SQL
