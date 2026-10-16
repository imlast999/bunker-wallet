# Bunker & CleanVault — Backend Setup & Deployment Guide

This document describes how to deploy the serverless edge backend using **Neon Serverless Postgres** and **Cloudflare Workers (Hono)**, and how to connect the **Bunker Android Wallet** application.

---

## Architecture Overview

```
┌─────────────────────────────────┐
│     Bunker Android App          │  • Non-custodial key custody
│  (Kotlin + Jetpack Compose)     │  • No database credentials on-device
└────────────────┬────────────────┘  • No raw SQL queries
                 │
                 │ HTTPS REST (Ktor Client)
                 │ Zero-secrets / Non-custodial payloads
                 ▼
┌─────────────────────────────────┐
│ Cloudflare Edge Worker (Hono)   │  • Low-latency edge execution (< 25ms)
│      `backend/src/index.ts`     │  • CORS-enabled API
└────────────────┬────────────────┘  • Parameterized SQL queries
                 │
                 │ @neondatabase/serverless (HTTP / WebSocket)
                 │ Encrypted connection via DATABASE_URL secret
                 ▼
┌─────────────────────────────────┐
│     Neon Serverless Postgres    │  • Branching, autoscaling, scale-to-zero
│         `schema.sql`            │  • PGP/UUID extensions
└─────────────────────────────────┘
```

---

## Step 1: Database Setup & Migrations (Neon Postgres)

### 1.1 Create a Neon Project
1. Log in to [Neon Console](https://console.neon.tech).
2. Create a new project (e.g. `bunker-wallet-db`).
3. Select your preferred region (e.g., `us-east-2` or `eu-central-1`).
4. Save your **Connection String (Pooled or Direct)**. It will look like:
   ```text
   postgresql://bunker_owner:AbCdEf123456@ep-sample-pool-123456.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```

### 1.2 Run Migrations
Run the schema script located at `backend/schema.sql`. You can execute it via either method below:

#### Method A: Neon Web SQL Editor (Recommended)
1. In the Neon Console, click **SQL Editor** on the left navigation bar.
2. Open `backend/schema.sql`, copy all contents, paste into the editor, and click **Run**.
3. Verify that the two tables were created:
   - `users`: Stores EVM and Solana public addresses and onboarding experience level (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`).
   - `scheduled_orders`: Stores off-hours scheduled trade intents and execution state.

#### Method B: psql CLI
```bash
psql "postgresql://bunker_owner:PASSWORD@ep-sample-pool-123456.us-east-2.aws.neon.tech/neondb?sslmode=require" -f backend/schema.sql
```

---

## Step 2: Deploy Cloudflare Worker (`backend/`)

The backend is built with [Hono](https://hono.dev/) and [`@neondatabase/serverless`](https://github.com/neondatabase/serverless).

### 2.1 Install Dependencies
Navigate to the `backend/` directory and install packages:
```bash
cd backend
npm install
```

### 2.2 Configure Neon Connection Secret
Set the `DATABASE_URL` secret in Cloudflare Wrangler:
```bash
npx wrangler secret put DATABASE_URL
```
When prompted, paste your full Neon connection string:
```text
postgresql://bunker_owner:PASSWORD@ep-sample-pool-123456.us-east-2.aws.neon.tech/neondb?sslmode=require
```

### 2.3 Test Locally (Optional)
To test locally, create a `backend/.dev.vars` file (ignored by Git):
```ini
DATABASE_URL=postgresql://bunker_owner:PASSWORD@ep-sample-pool-123456.us-east-2.aws.neon.tech/neondb?sslmode=require
```
Then run:
```bash
npm run dev
```
The local server will start at `http://localhost:8787`. You can test the health check:
```bash
curl http://localhost:8787/health
```

### 2.4 Deploy to Cloudflare Edge
Deploy the worker:
```bash
npx wrangler deploy
```
Upon completion, Wrangler will output your live URL:
```text
Published bunker-backend-api (https://bunker-backend-api.<your-subdomain>.workers.dev)
```

---

## Step 2B: Deploy to Vercel (Direct Serverless / Neon Integration)

The backend also includes native Vercel Edge Serverless integration via `backend/api/index.ts` and root `vercel.json`.

### 2B.1 Deploy via Vercel CLI or Git
1. Install or run Vercel CLI:
   ```bash
   npx vercel
   ```
2. Or connect your GitHub repository directly to Vercel:
   - Root Directory: Leave as repository root `./` (the root `vercel.json` rewrites `/api/*` and `/health` to `/backend/api/index`).

### 2B.2 Configure Neon Integration or Environment Variables
- If using the **Neon Vercel Integration** from the Vercel Marketplace, `POSTGRES_URL` and `DATABASE_URL` are injected automatically into your project.
- If configuring manually, go to **Project Settings > Environment Variables** and add:
  - `DATABASE_URL`: `postgresql://bunker_owner:PASSWORD@ep-sample-pool-123456.us-east-2.aws.neon.tech/neondb?sslmode=require`

### 2B.3 Verify Vercel Deployment
```bash
curl https://<your-project>.vercel.app/health
```

---

## Step 3: Connect Android App to the Backend API

### 3.1 Point App to the Deployed Endpoint
In the root directory of the Android project, open or create `local.properties`:

```properties
sdk.dir=C:/Users/your-username/AppData/Local/Android/Sdk

# Set your deployed Cloudflare Worker endpoint
backend.api.url=https://bunker-backend-api.<your-subdomain>.workers.dev
```

> **Note for Local Testing on Android Emulator:**
> If you are running `npm run dev` locally and testing via the Android Emulator, set:
> ```properties
> backend.api.url=http://10.0.2.2:8787
> ```

### 3.2 Compile and Verify
Rebuild the Android application:
```bat
build_apk.bat
```
or via Gradle:
```bash
./gradlew assembleDebug
```
The Gradle build reads `backend.api.url` from `local.properties` (with a secure fallback to `https://bunker-api.workers.dev`) and injects it into `BuildConfig.BACKEND_API_URL`.

---

## Step 4: Verification & API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Worker runtime status and version |
| `GET` | `/health` | Live connection probe against Neon Postgres |
| `POST` | `/api/users/sync` | Upserts public wallet addresses and experience level |
| `POST` | `/api/orders` | Saves or cancels off-hours scheduled trade intents |
| `GET` | `/api/orders?address={wallet}` | Fetches active scheduled orders for the wallet |

### Sample User Profile Sync
```bash
curl -X POST https://bunker-backend-api.<your-subdomain>.workers.dev/api/users/sync \
  -H "Content-Type: application/json" \
  -d '{
    "evm_address": "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
    "solana_address": "8k4k...XyZ",
    "experience_level": "INTERMEDIATE"
  }'
```

### Sample Scheduled Order
```bash
curl -X POST https://bunker-backend-api.<your-subdomain>.workers.dev/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "id": "11111111-2222-3333-4444-555555555555",
    "user_address": "0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
    "from_token": "USDC",
    "to_token": "bCSPX",
    "chain": "SOLANA",
    "amount_in": 250.0,
    "status": "PENDING",
    "trigger_epoch": 1776346200000
  }'
```

---

## Security Invariants

1. **Non-Custodial Integrity:** Private keys, seeds, and biometric secrets never leave the Android device hardware Keystore. The backend receives only public addresses and user interface preferences.
2. **Zero In-App Database Credentials:** The mobile application contains no database passwords, connection strings, or raw SQL. All persistence queries are executed server-side via parameterized SQL inside Cloudflare Workers.
3. **Branching & Scale-to-Zero:** Neon computes scale down automatically when idle, eliminating idle database hosting costs while maintaining sub-second cold starts.
