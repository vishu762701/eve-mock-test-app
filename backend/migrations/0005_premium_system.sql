-- ============================================================================
-- D1 Migration 0005: Automated Premium Purchase & Verification System
-- Database: eve-database
-- ============================================================================

-- 1. Premium Plan & Payment Settings
CREATE TABLE IF NOT EXISTS premium_config (
    id TEXT PRIMARY KEY DEFAULT 'default',
    is_enabled INTEGER NOT NULL DEFAULT 1,
    plan_name TEXT NOT NULL DEFAULT 'Premium Pro',
    price_inr INTEGER NOT NULL DEFAULT 99,
    currency TEXT NOT NULL DEFAULT 'INR',
    duration_days INTEGER NOT NULL DEFAULT 30,
    is_lifetime INTEGER NOT NULL DEFAULT 0,
    description TEXT NOT NULL DEFAULT 'Unlock all eligible tests & unlimited reattempts without the 3-attempt restriction.',
    benefits_json TEXT NOT NULL DEFAULT '["Access to all eligible tests","Unlimited eligible reattempts","No normal 3-attempt restriction while Premium is active"]',
    qr_enabled INTEGER NOT NULL DEFAULT 1,
    upi_enabled INTEGER NOT NULL DEFAULT 1,
    session_expiry_minutes INTEGER NOT NULL DEFAULT 10,
    merchant_vpa TEXT NOT NULL DEFAULT 'evemocktest@upi',
    merchant_name TEXT NOT NULL DEFAULT 'Eve Mock Test',
    webhook_secret TEXT NOT NULL DEFAULT 'eve_whsec_dev',
    updated_at INTEGER NOT NULL DEFAULT 0,
    updated_by TEXT NOT NULL DEFAULT 'admin'
);

INSERT OR IGNORE INTO premium_config (
    id, is_enabled, plan_name, price_inr, currency, duration_days, is_lifetime,
    description, benefits_json, qr_enabled, upi_enabled, session_expiry_minutes,
    merchant_vpa, merchant_name, webhook_secret, updated_at, updated_by
) VALUES (
    'default', 1, 'Premium Pro', 99, 'INR', 30, 0,
    'Unlock all eligible tests & unlimited reattempts without the 3-attempt restriction.',
    '["Access to all eligible tests","Unlimited eligible reattempts","No normal 3-attempt restriction while Premium is active"]',
    1, 1, 10, 'evemocktest@upi', 'Eve Mock Test', 'eve_whsec_dev', 0, 'admin'
);

-- 2. Premium Orders
CREATE TABLE IF NOT EXISTS premium_orders (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    user_email TEXT NOT NULL DEFAULT '',
    plan_id TEXT NOT NULL DEFAULT 'default',
    plan_name TEXT NOT NULL,
    amount INTEGER NOT NULL,
    currency TEXT NOT NULL DEFAULT 'INR',
    duration_days INTEGER NOT NULL DEFAULT 30,
    is_lifetime INTEGER NOT NULL DEFAULT 0,
    payment_method TEXT NOT NULL,
    provider_order_id TEXT NOT NULL DEFAULT '',
    provider_payment_id TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'CREATED',
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    paid_at INTEGER NOT NULL DEFAULT 0,
    metadata_json TEXT NOT NULL DEFAULT '{}'
);

CREATE INDEX IF NOT EXISTS idx_premium_orders_user ON premium_orders(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_premium_orders_status ON premium_orders(status);

-- 3. Premium Entitlements
CREATE TABLE IF NOT EXISTS premium_entitlements (
    user_id TEXT PRIMARY KEY,
    plan_id TEXT NOT NULL DEFAULT 'default',
    plan_name TEXT NOT NULL DEFAULT 'Premium Pro',
    payment_order_id TEXT NOT NULL DEFAULT '',
    payment_provider_id TEXT NOT NULL DEFAULT '',
    activated_at INTEGER NOT NULL,
    expires_at INTEGER,
    is_lifetime INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    source TEXT NOT NULL DEFAULT 'PAYMENT',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_premium_entitlements_status ON premium_entitlements(status, expires_at);
