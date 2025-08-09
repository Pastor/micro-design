CREATE DATABASE "event-sourcing" ENCODING 'UTF-8';
\c "event-sourcing"
CREATE SCHEMA tickets;
SET search_path = 'tickets';

CREATE TABLE tickets
(
  id SERIAL PRIMARY KEY
);

CREATE TABLE reserved
(
  ticket_id  BIGINT    NOT NULL REFERENCES tickets (id),
  user_id    BIGINT    NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (ticket_id)
);
CREATE INDEX reserves_user_id_idx ON reserved (user_id);

CREATE TABLE associated
(
  ticket_id           BIGINT    NOT NULL REFERENCES tickets (id),
  user_id             BIGINT    NOT NULL,
  reason_type         VARCHAR   NOT NULL DEFAULT 'INVOICING',
  reason_reference_id BIGINT    NOT NULL,
  created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (ticket_id)
);
CREATE INDEX associated_user_id_idx ON associated (user_id);

CREATE SCHEMA payments;
SET search_path = 'payments';

CREATE TABLE invoices
(
  id              SERIAL PRIMARY KEY,
  status          VARCHAR   NOT NULL DEFAULT 'CREATED' CHECK ( status IN ('CREATED', 'SENT_PAYMENT', 'PAYMENT', 'CANCELED') ),
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_payment_at TIMESTAMP,
  completed_at    TIMESTAMP
);

CREATE TABLE invoice_tickets
(
  ticket_id  BIGINT NOT NULL,
  invoice_id BIGINT NOT NULL REFERENCES invoices (id),
  price      BIGINT NOT NULL,
  UNIQUE (ticket_id)
);

CREATE TABLE payments
(
  id           SERIAL    NOT NULL PRIMARY KEY,
  price        BIGINT    NOT NULL,
  status       VARCHAR   NOT NULL DEFAULT 'CREATED' CHECK ( status IN ('CREATED', 'PAYMENT', 'CANCELED') ),
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP
)
