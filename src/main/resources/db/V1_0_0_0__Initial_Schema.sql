CREATE TABLE entry_cache (
  -- Generated by dSense
  id                    UUID      NOT NULL, -- Technical ID
  entry_date            TIMESTAMP NOT NULL, -- Entry timestamp when received by dSense

  -- From the entry parameters
  client_id             VARCHAR   NOT NULL, -- Client ID

  -- From the oracle data
  oracle_address        VARCHAR   NOT NULL, -- Hex address of the Oracle
  message_hash          VARCHAR   NOT NULL, -- H(Oracle counter + Oracle ticks)
  node_id               BIGINT    NOT NULL, -- Oracle secured element ID - 64 bits
  oracle_counter        BIGINT    NOT NULL, -- Oracle message counter (equivalent to nonce) - 64 bits
  oracle_ticks          INT       NOT NULL, -- Number of ticks since last entry

  PRIMARY KEY (id),
  UNIQUE (oracle_address, message_hash)
);

CREATE TABLE entry_history (
  -- Generated by dSense
  id                    UUID      NOT NULL, -- Technical ID
  entry_date            TIMESTAMP NOT NULL, -- Entry timestamp when received by dSense
  entry_hash            VARCHAR   NOT NULL, -- H(Oracle address + message hash)

  -- From the entry parameters
  client_id             VARCHAR   NOT NULL, -- Client ID

  -- From the oracle data
  node_id               BIGINT    NOT NULL, -- Oracle secured element ID - 64 bits
  oracle_counter        BIGINT    NOT NULL, -- Oracle message counter (equivalent to nonce) - 64 bits

  PRIMARY KEY (id),
  UNIQUE (entry_hash)
);
