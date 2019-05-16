DROP TABLE pending_entry;

CREATE TABLE pending_entry (
  entry_cache_id        UUID,             -- Reference to the pending entry in cache
  pending_tx            VARCHAR NOT NULL, -- Pending transaction

  PRIMARY KEY (entry_cache_id),
  FOREIGN KEY (entry_cache_id) REFERENCES entry_cache(id) ON DELETE CASCADE
);
