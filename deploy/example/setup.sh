#!/bin/sh
# First-time server directories
#   DEPLOY_ROOT=/var/www/html/tcgshop ./deploy/example/setup.sh

set -e
ROOT="${DEPLOY_ROOT:-/var/www/html/tcgshop}"

mkdir -p "$ROOT/data/uploads" "$ROOT/data/card-images" "$ROOT/data/mysql" "$ROOT/logs"
chmod 755 "$ROOT" "$ROOT/data" "$ROOT/data/uploads" "$ROOT/data/card-images" "$ROOT/data/mysql" "$ROOT/logs"

echo "Created under $ROOT:"
echo "  data/uploads"
echo "  data/card-images"
echo "  logs"
echo "  data/mysql"
