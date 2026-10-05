# ADR 005: Link expiry

**Run:** e14206683a0e

## Decision
expires_at nullable; default TTL from app.link.default-ttl-days when ttlDays is not given; expired links return 410.

## Rationale
Checked at resolve time against Postgres/cached expiry so correctness does not depend on cleanup; cache TTL is capped at remaining lifetime.

## Alternatives considered
Hard delete by cron (gives 404 not 410); Redis TTL only (loses mapping).
