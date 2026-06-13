ALTER TABLE bookings ADD COLUMN cancel_token UUID NOT NULL DEFAULT gen_random_uuid();

CREATE UNIQUE INDEX idx_bookings_cancel_token ON bookings(cancel_token);
