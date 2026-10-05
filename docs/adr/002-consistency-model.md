# ADR 002: Consistency model

**Run:** e14206683a0e

## Decision
Strong for mapping (Postgres is source of truth, write-through before returning 201); eventual for clicks.

## Rationale
Redirect does Redis INCR on click:{code} and never blocks on analytics; a scheduled job flushes deltas to links.click_count.

## Alternatives considered
Synchronous DB increment per click (adds latency and row contention); Kafka event pipeline (overkill for v1).
