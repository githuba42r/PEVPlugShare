import express, { type Request, type Response } from 'express';
import request from 'supertest';
import { createRateLimiter } from '../../src/middleware/rate-limiter';
import { MemoryCache } from '../../src/cache/memory-cache';
import type { RateLimitRepository } from '../../src/repositories/interfaces';
import type { ResolvedRateLimit } from '../../src/models/rate-limit';

function makeMockRepo(resolved: ResolvedRateLimit | null): RateLimitRepository {
  return {
    create: jest.fn(),
    getById: jest.fn(),
    update: jest.fn(),
    delete: jest.fn(),
    getAll: jest.fn(),
    getByScope: jest.fn(),
    resolve: jest.fn().mockResolvedValue(resolved),
  };
}

function makeApp(
  cache: MemoryCache,
  repo: RateLimitRepository,
  userId?: string,
) {
  const app = express();
  app.use(express.json());

  // Simulate auth middleware setting userId
  app.use((req: Request, _res: Response, next) => {
    if (userId) {
      (req as Request & { userId: string }).userId = userId;
    }
    next();
  });

  app.use(createRateLimiter(cache, repo));

  app.get('/test', (_req: Request, res: Response) => {
    res.json({ ok: true });
  });

  return app;
}

describe('createRateLimiter middleware', () => {
  it('allows requests when no rate limit is configured (resolve returns null)', async () => {
    const cache = new MemoryCache();
    const repo = makeMockRepo(null);
    const app = makeApp(cache, repo, 'user-1');

    const res = await request(app).get('/test');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ ok: true });
  });

  it('allows requests when there is no userId (unauthenticated)', async () => {
    const cache = new MemoryCache();
    const repo = makeMockRepo({ endpoint: 'GET /test', maxRequests: 2, windowSeconds: 60, scope: 'GLOBAL', scopeId: null });
    const app = makeApp(cache, repo, undefined);

    const res = await request(app).get('/test');
    expect(res.status).toBe(200);
    // resolve should NOT be called when there is no userId
    expect(repo.resolve).not.toHaveBeenCalled();
  });

  it('allows requests within the rate limit and sets headers', async () => {
    const cache = new MemoryCache();
    const repo = makeMockRepo({
      endpoint: 'GET /test',
      maxRequests: 5,
      windowSeconds: 60,
      scope: 'GLOBAL',
      scopeId: null,
    });
    const app = makeApp(cache, repo, 'user-2');

    const res = await request(app).get('/test');
    expect(res.status).toBe(200);
    expect(res.headers['x-ratelimit-limit']).toBe('5');
    expect(res.headers['x-ratelimit-remaining']).toBe('4');
  });

  it('blocks requests exceeding the rate limit with 429', async () => {
    const cache = new MemoryCache();
    const repo = makeMockRepo({
      endpoint: 'GET /test',
      maxRequests: 2,
      windowSeconds: 60,
      scope: 'USER',
      scopeId: 'user-3',
    });
    const app = makeApp(cache, repo, 'user-3');

    // First two requests should succeed
    await request(app).get('/test').expect(200);
    await request(app).get('/test').expect(200);

    // Third request should be blocked
    const res = await request(app).get('/test');
    expect(res.status).toBe(429);
    expect(res.body).toHaveProperty('error');
    expect(res.body).toHaveProperty('retryAfter');
  });

  it('includes X-RateLimit-Limit and X-RateLimit-Remaining headers on blocked request', async () => {
    const cache = new MemoryCache();
    const repo = makeMockRepo({
      endpoint: 'GET /test',
      maxRequests: 1,
      windowSeconds: 60,
      scope: 'GLOBAL',
      scopeId: null,
    });
    const app = makeApp(cache, repo, 'user-4');

    await request(app).get('/test').expect(200);

    const res = await request(app).get('/test');
    expect(res.status).toBe(429);
    expect(res.headers['x-ratelimit-limit']).toBe('1');
    expect(res.headers['x-ratelimit-remaining']).toBe('0');
  });
});
