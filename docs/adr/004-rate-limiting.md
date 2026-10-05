# ADR 004: Rate limiting

**Run:** e14206683a0e

## Decision
Fixed-window per-client-IP counter in Redis (INCR + EXPIRE), 429 with Retry-After.

## Rationale
Simple, shared across instances, cheap. Fails open if Redis is unavailable.

## Alternatives considered
Token bucket via Bucket4j (smoother, more complex); gateway-level limiting.
