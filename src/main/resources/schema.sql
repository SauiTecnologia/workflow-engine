-- Script para criar tabelas do Workflow Service no Supabase

-- Tabela: pipelines
CREATE TABLE IF NOT EXISTS pipelines (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    context_type TEXT NOT NULL,
    context_id TEXT NOT NULL,
    allowed_roles_view JSONB,
    allowed_roles_manage JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(context_type, context_id)
);

-- Tabela: pipeline_columns
CREATE TABLE IF NOT EXISTS pipeline_columns (
    id BIGSERIAL PRIMARY KEY,
    pipeline_id BIGINT NOT NULL REFERENCES pipelines(id) ON DELETE CASCADE,
    key TEXT NOT NULL,
    name TEXT NOT NULL,
    position INTEGER NOT NULL,
    allowed_entity_types JSONB,
    allowed_roles_view JSONB,
    allowed_roles_move_in JSONB,
    allowed_roles_move_out JSONB,
    transition_rules_json JSONB,
    notification_rules_json JSONB,
    card_layout_json JSONB,
    filter_config_json JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(pipeline_id, key)
);

-- Tabela: pipeline_cards
CREATE TABLE IF NOT EXISTS pipeline_cards (
    id BIGSERIAL PRIMARY KEY,
    pipeline_id BIGINT NOT NULL REFERENCES pipelines(id) ON DELETE CASCADE,
    column_id BIGINT NOT NULL REFERENCES pipeline_columns(id) ON DELETE CASCADE,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    sort_order INTEGER,
    data_snapshot_json JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(pipeline_id, entity_type, entity_id)
);

-- Índices para melhor performance
CREATE INDEX idx_pipelines_context ON pipelines(context_type, context_id);
CREATE INDEX idx_pipeline_columns_pipeline_id ON pipeline_columns(pipeline_id);
CREATE INDEX idx_pipeline_cards_pipeline_id ON pipeline_cards(pipeline_id);
CREATE INDEX idx_pipeline_cards_column_id ON pipeline_cards(column_id);
CREATE INDEX idx_pipeline_cards_entity ON pipeline_cards(entity_type, entity_id);

-- Comentários
COMMENT ON TABLE pipelines IS 'Representa um Kanban board (pipeline) para um contexto específico (ex: edital-123)';
COMMENT ON TABLE pipeline_columns IS 'Colunas dentro de um pipeline com regras configuráveis em JSON';
COMMENT ON TABLE pipeline_cards IS 'Cards que representam entidades (projetos, avaliações, etc) dentro de uma coluna';

COMMENT ON COLUMN pipelines.context_type IS 'Tipo de contexto: "edital", "chamada", etc';
COMMENT ON COLUMN pipelines.context_id IS 'ID do contexto: "edital-123", "chamada-2025", etc';

COMMENT ON COLUMN pipeline_columns.key IS 'Chave única da coluna: "inscritos", "em_avaliacao", etc';
COMMENT ON COLUMN pipeline_columns.transition_rules_json IS 'JSON com regras de transição entre colunas';
COMMENT ON COLUMN pipeline_columns.notification_rules_json IS 'JSON com regras de notificação ao entrar/sair da coluna';
COMMENT ON COLUMN pipeline_columns.card_layout_json IS 'JSON com layout do card (title, subtitle, tags, etc)';

COMMENT ON COLUMN pipeline_cards.entity_type IS 'Tipo de entidade: "project", "evaluation", etc';
COMMENT ON COLUMN pipeline_cards.entity_id IS 'ID da entidade no sistema domínio (ex: "proj-123")';
COMMENT ON COLUMN pipeline_cards.data_snapshot_json IS 'Snapshot dos dados da entidade no momento da criação do card';

-- Exemplos de dados para testes

-- 1. Criar um pipeline para edital
INSERT INTO pipelines (name, context_type, context_id, allowed_roles_view, allowed_roles_manage) VALUES (
    'Edital 2025 - Pipeline de Projetos',
    'edital',
    'edital-2025-001',
    '["admin", "gestor", "avaliador", "proponente"]'::jsonb,
    '["admin", "gestor"]'::jsonb
) ON CONFLICT (context_type, context_id) DO NOTHING;

-- 2. Criar colunas para o pipeline
INSERT INTO pipeline_columns 
(pipeline_id, key, name, position, allowed_entity_types, allowed_roles_view, allowed_roles_move_in, allowed_roles_move_out, transition_rules_json, notification_rules_json, card_layout_json) 
VALUES 
(
    (SELECT id FROM pipelines WHERE context_id = 'edital-2025-001'),
    'inscritos',
    'Inscritos',
    1,
    '["project"]'::jsonb,
    '["admin", "gestor", "avaliador", "proponente"]'::jsonb,
    '["admin", "gestor"]'::jsonb,
    '["admin", "gestor", "proponente"]'::jsonb,
    '{"transitions": [{"from": "inscritos", "to": "em_avaliacao", "allowedRoles": ["admin", "gestor"]}]}'::jsonb,
    '{"on_enter": [{"eventType": "PROJECT_CREATED", "channels": ["email"], "recipients": ["project_owner"]}]}'::jsonb,
    '{"title": "title", "subtitle": "ownerName", "tags": ["status"]}'::jsonb
),
(
    (SELECT id FROM pipelines WHERE context_id = 'edital-2025-001'),
    'em_avaliacao',
    'Em Avaliação',
    2,
    '["project"]'::jsonb,
    '["admin", "gestor", "avaliador"]'::jsonb,
    '["admin", "gestor", "avaliador"]'::jsonb,
    '["admin", "gestor"]'::jsonb,
    '{"transitions": [{"from": "em_avaliacao", "to": "aprovados", "allowedRoles": ["admin", "avaliador"]}, {"from": "em_avaliacao", "to": "rejeitados", "allowedRoles": ["admin", "avaliador"]}]}'::jsonb,
    '{"on_enter": [{"eventType": "PROJECT_READY_FOR_REVIEW", "channels": ["email"], "recipients": ["avaliador"]}]}'::jsonb,
    '{"title": "title", "subtitle": "ownerName", "tags": ["status", "score"]}'::jsonb
),
(
    (SELECT id FROM pipelines WHERE context_id = 'edital-2025-001'),
    'aprovados',
    'Aprovados',
    3,
    '["project"]'::jsonb,
    '["admin", "gestor", "avaliador", "proponente"]'::jsonb,
    NULL,
    '["admin", "gestor"]'::jsonb,
    NULL,
    '{"on_enter": [{"eventType": "PROJECT_APPROVED", "channels": ["email"], "recipients": ["project_owner"]}]}'::jsonb,
    '{"title": "title", "subtitle": "ownerName", "tags": ["status", "score"]}'::jsonb
),
(
    (SELECT id FROM pipelines WHERE context_id = 'edital-2025-001'),
    'rejeitados',
    'Rejeitados',
    4,
    '["project"]'::jsonb,
    '["admin", "gestor", "avaliador"]'::jsonb,
    NULL,
    '["admin", "gestor"]'::jsonb,
    NULL,
    '{"on_enter": [{"eventType": "PROJECT_REJECTED", "channels": ["email"], "recipients": ["project_owner"]}]}'::jsonb,
    '{"title": "title", "subtitle": "ownerName", "tags": ["status"]}'::jsonb
) ON CONFLICT (pipeline_id, key) DO NOTHING;

-- 3. Criar alguns cards de exemplo
INSERT INTO pipeline_cards (pipeline_id, column_id, entity_type, entity_id, sort_order, data_snapshot_json) VALUES
(
    (SELECT id FROM pipelines WHERE context_id = 'edital-2025-001'),
    (SELECT id FROM pipeline_columns WHERE key = 'inscritos' AND pipeline_id = (SELECT id FROM pipelines WHERE context_id = 'edital-2025-001')),
    'project',
    'proj-001',
    1,
    '{"title": "Projeto A", "ownerName": "Maria Silva", "status": "Inscrito"}'::jsonb
),
(
    (SELECT id FROM pipelines WHERE context_id = 'edital-2025-001'),
    (SELECT id FROM pipeline_columns WHERE key = 'inscritos' AND pipeline_id = (SELECT id FROM pipelines WHERE context_id = 'edital-2025-001')),
    'project',
    'proj-002',
    2,
    '{"title": "Projeto B", "ownerName": "João Souza", "status": "Inscrito"}'::jsonb
),
(
    (SELECT id FROM pipelines WHERE context_id = 'edital-2025-001'),
    (SELECT id FROM pipeline_columns WHERE key = 'em_avaliacao' AND pipeline_id = (SELECT id FROM pipelines WHERE context_id = 'edital-2025-001')),
    'project',
    'proj-003',
    1,
    '{"title": "Projeto C", "ownerName": "Ana Costa", "status": "Em avaliação", "score": 8.5}'::jsonb
) ON CONFLICT (pipeline_id, entity_type, entity_id) DO NOTHING;

-- Verificar dados inseridos
SELECT 'Pipelines:' as info, COUNT(*) FROM pipelines;
SELECT 'Colunas:' as info, COUNT(*) FROM pipeline_columns;
SELECT 'Cards:' as info, COUNT(*) FROM pipeline_cards;
