#!/bin/sh
# Print useful Spring/Hibernate DB validation errors.
# Usage:
#   cd /var/www/html/tcgshop
#   ./show-db-validation-log.sh

set -eu

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.demo.yml}"
SERVICE="${SERVICE:-app}"
LOG_FILE="${LOG_FILE:-./logs/application.log}"
PATTERN='Schema-validation|SchemaManagementException|missing table|missing column|wrong column type|Unable to build Hibernate SessionFactory|ddl-auto|Database|JDBC|SQLSyntaxErrorException|SQLGrammarException|BeanCreationException|Application run failed'

echo "== docker compose logs: $SERVICE =="
if command -v docker >/dev/null 2>&1; then
  docker compose -f "$COMPOSE_FILE" logs --tail=300 "$SERVICE" 2>/dev/null \
    | grep -Ei "$PATTERN" || true
fi

echo
echo "== file log: $LOG_FILE =="
if [ -f "$LOG_FILE" ]; then
  tail -n 500 "$LOG_FILE" | grep -Ei "$PATTERN" || true
else
  echo "No file log found yet: $LOG_FILE"
fi

echo
echo "Tip: for full live logs, run:"
echo "  docker compose -f $COMPOSE_FILE logs -f --tail=200 $SERVICE"
echo "  tail -f $LOG_FILE"
