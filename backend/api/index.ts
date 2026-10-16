import { handle } from 'hono/vercel';
import app from '../src/index';

// Export Edge Runtime configuration for Vercel
export const config = {
  runtime: 'edge',
};

// Export HTTP method handlers supported by Hono's Vercel adapter
export const GET = handle(app);
export const POST = handle(app);
export const PUT = handle(app);
export const DELETE = handle(app);
export const PATCH = handle(app);
export const OPTIONS = handle(app);

// Default export for serverless compatibility
export default handle(app);
