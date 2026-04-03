import type { Request, Response, NextFunction } from 'express';
import type { Cache } from '../cache/interfaces';
import type { RateLimitRepository } from '../repositories/interfaces';

export function createRateLimiter(cache: Cache, rateLimitRepo: RateLimitRepository) {
  return async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      // userId is set by auth middleware (not yet implemented); skip if unauthenticated
      const userId: string | undefined = (req as Request & { userId?: string }).userId;
      if (!userId) {
        next();
        return;
      }

      const endpoint = `${req.method} ${req.path}`;
      const userGroupIds: string[] = (req as Request & { userGroupIds?: string[] }).userGroupIds ?? [];

      const limit = await rateLimitRepo.resolve(userId, endpoint, userGroupIds);
      if (!limit) {
        next();
        return;
      }

      const cacheKey = `ratelimit:${userId}:${endpoint}`;
      const current = await cache.increment(cacheKey, limit.windowSeconds);

      res.setHeader('X-RateLimit-Limit', String(limit.maxRequests));
      res.setHeader('X-RateLimit-Remaining', String(Math.max(0, limit.maxRequests - current)));

      if (current > limit.maxRequests) {
        res.status(429).json({
          error: 'Too many requests',
          retryAfter: limit.windowSeconds,
        });
        return;
      }

      next();
    } catch (err) {
      console.error('Rate limiter error (failing open):', err);
      next();
    }
  };
}
