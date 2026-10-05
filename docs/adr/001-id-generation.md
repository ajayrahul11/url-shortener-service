# ADR 001: ID generation

**Run:** e14206683a0e

## Decision
Atomic Redis INCR counter encoded as base62.

## Rationale
Zero collisions by construction and short codes; one network hop. Counter persisted via Redis AOF and seeded from max(id) in Postgres at startup.

## Alternatives considered
Hash of URL (collision handling, retries, longer codes); Snowflake IDs (scale across nodes without coordination but 11+ char codes). Redis counter is a shared dependency, so scale-out is fine for app nodes but Redis is the single point; mitigate with block allocation later.
