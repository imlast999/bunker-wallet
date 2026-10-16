-- Neon Postgres Schema for CleanVault / Bunker Wallet
-- Run this script in the Neon SQL Editor or via psql / Neon CLI

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evm_address TEXT UNIQUE NOT NULL,
    solana_address TEXT UNIQUE NOT NULL,
    experience_level VARCHAR(20) DEFAULT 'INTERMEDIATE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_active TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_evm_address ON users(evm_address);
CREATE INDEX IF NOT EXISTS idx_users_solana_address ON users(solana_address);

CREATE TABLE IF NOT EXISTS scheduled_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    from_token VARCHAR(30) NOT NULL,
    to_token VARCHAR(30) NOT NULL,
    chain VARCHAR(20) NOT NULL,
    amount_in NUMERIC NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    trigger_epoch BIGINT NOT NULL,
    intent_hash TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scheduled_orders_user_id ON scheduled_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_orders_status ON scheduled_orders(status);
CREATE INDEX IF NOT EXISTS idx_scheduled_orders_trigger_epoch ON scheduled_orders(trigger_epoch);
