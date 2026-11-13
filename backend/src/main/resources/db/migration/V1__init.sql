CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE users(id UUID PRIMARY KEY DEFAULT gen_random_uuid(), email TEXT NOT NULL UNIQUE,
  display_name TEXT NOT NULL, password_hash TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE refresh_tokens(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, token_hash TEXT NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL, revoked BOOLEAN NOT NULL DEFAULT false);
CREATE TABLE integrations(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  provider TEXT NOT NULL CHECK (provider IN ('GITHUB','GMAIL','CALENDAR','CLASSROOM')),
  status TEXT NOT NULL DEFAULT 'DISCONNECTED' CHECK (status IN ('CONNECTED','DISCONNECTED','ERROR')),
  account_label TEXT, last_sync_at TIMESTAMPTZ, last_error TEXT, UNIQUE(user_id, provider));
CREATE TABLE oauth_tokens(integration_id UUID PRIMARY KEY REFERENCES integrations(id) ON DELETE CASCADE,
  access_token_enc TEXT NOT NULL, refresh_token_enc TEXT, scopes TEXT, expires_at TIMESTAMPTZ);
CREATE TABLE projects(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, name TEXT NOT NULL,
  description TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE activities(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, source TEXT NOT NULL,
  external_id TEXT NOT NULL, activity_type TEXT NOT NULL, title TEXT NOT NULL, description TEXT,
  source_url TEXT, occurred_at TIMESTAMPTZ NOT NULL, imported_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  search_vec tsvector GENERATED ALWAYS AS (to_tsvector('english', coalesce(title,'')||' '||coalesce(description,''))) STORED,
  UNIQUE(user_id, source, external_id));
CREATE INDEX idx_activities_timeline ON activities(user_id, occurred_at DESC);
CREATE INDEX idx_activities_search ON activities USING GIN(search_vec);
CREATE TABLE tasks(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  project_id UUID REFERENCES projects(id) ON DELETE SET NULL, title TEXT NOT NULL, notes TEXT,
  status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED')),
  priority INT NOT NULL DEFAULT 2 CHECK (priority BETWEEN 1 AND 3), due_at TIMESTAMPTZ,
  is_suggestion BOOLEAN NOT NULL DEFAULT false, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE INDEX idx_tasks_user_due ON tasks(user_id, due_at);
CREATE TABLE task_sources(task_id UUID REFERENCES tasks(id) ON DELETE CASCADE,
  activity_id UUID REFERENCES activities(id) ON DELETE CASCADE, PRIMARY KEY(task_id, activity_id));
CREATE TABLE project_activities(project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
  activity_id UUID REFERENCES activities(id) ON DELETE CASCADE, PRIMARY KEY(project_id, activity_id));
CREATE TABLE activity_relations(from_id UUID REFERENCES activities(id) ON DELETE CASCADE,
  to_id UUID REFERENCES activities(id) ON DELETE CASCADE, reason TEXT NOT NULL,
  is_ai_suggested BOOLEAN NOT NULL DEFAULT false, PRIMARY KEY(from_id, to_id));
CREATE TABLE sync_state(integration_id UUID PRIMARY KEY REFERENCES integrations(id) ON DELETE CASCADE,
  cursor TEXT, updated_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE sync_history(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  integration_id UUID NOT NULL REFERENCES integrations(id) ON DELETE CASCADE,
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(), finished_at TIMESTAMPTZ,
  imported_count INT NOT NULL DEFAULT 0, success BOOLEAN, error TEXT);
CREATE TABLE notifications(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, message TEXT NOT NULL,
  read BOOLEAN NOT NULL DEFAULT false, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE ai_conversations(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE ai_messages(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
  role TEXT NOT NULL, content TEXT NOT NULL, referenced_activity_ids UUID[], created_at TIMESTAMPTZ NOT NULL DEFAULT now());
