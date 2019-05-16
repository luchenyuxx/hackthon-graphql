#!/usr/bin/env bash

set -euxo pipefail

exec java -cp /app/dsense-gateway.jar:/app/resources co.ledger.dsense.gateway.App
