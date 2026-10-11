CREATE TABLE raffles (
  id TEXT PRIMARY KEY, owner_hash TEXT NOT NULL, name TEXT NOT NULL,
  description TEXT NOT NULL, total INTEGER NOT NULL CHECK(total BETWEEN 1 AND 10000),
  sold TEXT NOT NULL DEFAULT '[]', closed INTEGER NOT NULL DEFAULT 0,
  image TEXT NOT NULL DEFAULT '', revision INTEGER NOT NULL DEFAULT 1,
  last_accept TEXT, updated_at TEXT NOT NULL
);
CREATE TABLE requests (
  id TEXT PRIMARY KEY, raffle_id TEXT NOT NULL REFERENCES raffles(id),
  name TEXT NOT NULL, phone TEXT NOT NULL DEFAULT '', numbers TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','accepted','rejected')),
  created_at TEXT NOT NULL
);
CREATE INDEX requests_raffle_created ON requests(raffle_id, created_at);
