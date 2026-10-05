# ADR 003: Caching

**Run:** e14206683a0e

## Decision
Cache-aside in Redis for redirect lookups, key link:{code}, TTL min(redirect-ttl, remaining lifetime).

## Rationale
Fast reads; Postgres fallback when Redis is down. Alias creation uses DB unique constraint, then evicts key; any future update/delete must evict link:{code} after commit.

## Alternatives considered
Write-through cache; in-process Caffeine (stale across instances).
