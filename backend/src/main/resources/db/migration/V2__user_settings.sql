ALTER TABLE users ADD COLUMN auto_sync BOOLEAN NOT NULL DEFAULT true,
                  ADD COLUMN ai_external_default BOOLEAN NOT NULL DEFAULT false;
