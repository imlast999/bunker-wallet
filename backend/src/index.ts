import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { neon } from '@neondatabase/serverless';

export type Bindings = {
  DATABASE_URL?: string;
  POSTGRES_URL?: string;
  ENVIRONMENT?: string;
};

/**
 * Resolves the Neon Postgres connection string from Cloudflare Worker env
 * or Vercel serverless environment variables (DATABASE_URL / POSTGRES_URL).
 */
export function getDatabaseUrl(env?: Partial<Bindings>): string | undefined {
  return env?.DATABASE_URL ||
    env?.POSTGRES_URL ||
    (typeof process !== 'undefined' ? (process.env?.DATABASE_URL || process.env?.POSTGRES_URL) : undefined);
}

export const app = new Hono<{ Bindings: Bindings }>();

// Enable CORS for mobile clients and frontend dashboards
app.use('*', cors());

// Health & Status check
app.get('/', (c) => {
  return c.json({
    service: 'Bunker / CleanVault Neon Backend API',
    status: 'online',
    version: '1.0.0',
    runtime: typeof process !== 'undefined' && process.env?.VERCEL ? 'Vercel Serverless' : 'Edge Runtime'
  });
});

app.get('/health', async (c) => {
  const dbUrl = getDatabaseUrl(c.env);
  if (!dbUrl) {
    return c.json({
      status: 'warning',
      database: 'DATABASE_URL or POSTGRES_URL environment variable missing'
    }, 503);
  }

  try {
    const sql = neon(dbUrl);
    const result = await sql`SELECT 1 as connected;`;
    return c.json({
      status: 'ok',
      database: 'connected',
      provider: 'Neon Postgres',
      result
    });
  } catch (error: any) {
    return c.json({ status: 'error', database: error.message }, 500);
  }
});

/**
 * POST /api/users/sync
 * Upserts user profile by evm_address and solana_address.
 * Updates experience_level (BEGINNER, INTERMEDIATE, ADVANCED) and last_active timestamp.
 */
app.post('/api/users/sync', async (c) => {
  const dbUrl = getDatabaseUrl(c.env);
  if (!dbUrl) {
    return c.json({ success: false, error: 'Database configuration missing (DATABASE_URL / POSTGRES_URL)' }, 500);
  }

  try {
    const body = await c.req.json();
    const evmAddress = (body.evm_address || body.evmAddress || '').trim().toLowerCase();
    const solanaAddress = (body.solana_address || body.solanaAddress || '').trim();
    const experienceLevel = (body.experience_level || body.experienceLevel || 'INTERMEDIATE').trim().toUpperCase();

    if (!evmAddress || !solanaAddress) {
      return c.json({
        success: false,
        error: 'Both evm_address and solana_address are required'
      }, 400);
    }

    const sql = neon(dbUrl);

    const rows = await sql`
      INSERT INTO users (evm_address, solana_address, experience_level, last_active)
      VALUES (${evmAddress}, ${solanaAddress}, ${experienceLevel}, CURRENT_TIMESTAMP)
      ON CONFLICT (evm_address)
      DO UPDATE SET
        solana_address = EXCLUDED.solana_address,
        experience_level = EXCLUDED.experience_level,
        last_active = CURRENT_TIMESTAMP
      RETURNING id, evm_address, solana_address, experience_level, created_at, last_active;
    `;

    return c.json({
      success: true,
      user: rows[0]
    });
  } catch (error: any) {
    return c.json({
      success: false,
      error: error.message || 'Failed to sync user profile'
    }, 500);
  }
});

/**
 * POST /api/orders
 * Saves a new scheduled order or updates/cancels an existing order.
 */
app.post('/api/orders', async (c) => {
  const dbUrl = getDatabaseUrl(c.env);
  if (!dbUrl) {
    return c.json({ success: false, error: 'Database configuration missing (DATABASE_URL / POSTGRES_URL)' }, 500);
  }

  try {
    const body = await c.req.json();
    const userAddress = (body.user_address || body.userAddress || '').trim();
    const orderId = body.id ? String(body.id) : crypto.randomUUID();
    const fromToken = body.from_token || body.fromToken || '';
    const toToken = body.to_token || body.toToken || '';
    const chain = body.chain || 'SOLANA';
    const amountIn = Number(body.amount_in ?? body.amountIn ?? 0);
    const status = (body.status || 'PENDING').trim().toUpperCase();
    const triggerEpoch = Number(body.trigger_epoch ?? body.triggerEpoch ?? Date.now());
    const intentHash = body.intent_hash || body.intentHash || null;

    if (!userAddress) {
      return c.json({ success: false, error: 'user_address is required' }, 400);
    }

    const sql = neon(dbUrl);

    // 1. Resolve user ID from evm_address or solana_address
    const users = await sql`
      SELECT id FROM users
      WHERE evm_address = ${userAddress.toLowerCase()} OR solana_address = ${userAddress}
      LIMIT 1;
    `;

    let userId: string;
    if (users.length > 0) {
      userId = users[0].id;
    } else {
      // Auto-provision placeholder user row if order arrives before sync
      const isEvm = userAddress.startsWith('0x');
      const newUser = await sql`
        INSERT INTO users (evm_address, solana_address, experience_level)
        VALUES (
          ${isEvm ? userAddress.toLowerCase() : 'placeholder_' + crypto.randomUUID().slice(0, 8)},
          ${!isEvm ? userAddress : 'placeholder_' + crypto.randomUUID().slice(0, 8)},
          'INTERMEDIATE'
        )
        RETURNING id;
      `;
      userId = newUser[0].id;
    }

    // 2. Validate UUID format for orderId or generate valid UUID
    const isValidUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(orderId);
    const finalOrderId = isValidUuid ? orderId : crypto.randomUUID();

    // 3. Upsert order
    const rows = await sql`
      INSERT INTO scheduled_orders (
        id, user_id, from_token, to_token, chain, amount_in, status, trigger_epoch, intent_hash, created_at
      ) VALUES (
        ${finalOrderId}, ${userId}, ${fromToken}, ${toToken}, ${chain}, ${amountIn}, ${status}, ${triggerEpoch}, ${intentHash}, CURRENT_TIMESTAMP
      )
      ON CONFLICT (id)
      DO UPDATE SET
        status = EXCLUDED.status,
        intent_hash = COALESCE(EXCLUDED.intent_hash, scheduled_orders.intent_hash)
      RETURNING id, user_id, from_token, to_token, chain, amount_in, status, trigger_epoch, intent_hash, created_at;
    `;

    return c.json({
      success: true,
      order: rows[0]
    });
  } catch (error: any) {
    return c.json({
      success: false,
      error: error.message || 'Failed to save scheduled order'
    }, 500);
  }
});

/**
 * GET /api/orders?address=...
 * Retrieves active/all scheduled orders for a specific user wallet.
 */
app.get('/api/orders', async (c) => {
  const dbUrl = getDatabaseUrl(c.env);
  if (!dbUrl) {
    return c.json({ success: false, error: 'Database configuration missing (DATABASE_URL / POSTGRES_URL)' }, 500);
  }

  const address = c.req.query('address');
  const statusFilter = c.req.query('status'); // Optional: 'PENDING', 'CANCELLED', etc.

  if (!address) {
    return c.json({ success: false, error: 'Query parameter "address" is required' }, 400);
  }

  try {
    const sql = neon(dbUrl);

    // Find user
    const users = await sql`
      SELECT id FROM users
      WHERE evm_address = ${address.toLowerCase()} OR solana_address = ${address}
      LIMIT 1;
    `;

    if (users.length === 0) {
      return c.json({ success: true, count: 0, orders: [] });
    }

    const userId = users[0].id;

    let orders;
    if (statusFilter) {
      orders = await sql`
        SELECT id, user_id, from_token, to_token, chain, amount_in, status, trigger_epoch, intent_hash, created_at
        FROM scheduled_orders
        WHERE user_id = ${userId} AND status = ${statusFilter.toUpperCase()}
        ORDER BY trigger_epoch ASC;
      `;
    } else {
      orders = await sql`
        SELECT id, user_id, from_token, to_token, chain, amount_in, status, trigger_epoch, intent_hash, created_at
        FROM scheduled_orders
        WHERE user_id = ${userId}
        ORDER BY trigger_epoch ASC;
      `;
    }

    return c.json({
      success: true,
      count: orders.length,
      orders
    });
  } catch (error: any) {
    return c.json({
      success: false,
      error: error.message || 'Failed to retrieve scheduled orders'
    }, 500);
  }
});

export default app;
