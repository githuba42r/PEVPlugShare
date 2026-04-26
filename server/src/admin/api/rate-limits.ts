import { Router } from 'express';
import type { Repositories } from '../../repositories/factory';
import { AppError } from '../../middleware/error-handler';

export function createAdminRateLimitRouter(repos: Repositories): Router {
  const router = Router();

  // GET /rate-limits/resolve/:userId — MUST be before /:id
  router.get('/rate-limits/resolve/:userId', async (req, res, next) => {
    try {
      const user = await repos.users.getById(req.params.userId);
      if (!user) {
        return next(new AppError(404, 'User not found'));
      }
      const resolved = await repos.rateLimits.resolve(req.params.userId, '*', []);
      if (!resolved) {
        return res.json({ message: 'No rate limit applies' });
      }
      res.json(resolved);
    } catch (err) {
      next(err);
    }
  });

  // GET /rate-limits — list all rate limits
  router.get('/rate-limits', async (_req, res, next) => {
    try {
      const limits = await repos.rateLimits.getAll();
      res.json(limits);
    } catch (err) {
      next(err);
    }
  });

  // POST /rate-limits — create rate limit
  router.post('/rate-limits', async (req, res, next) => {
    try {
      const { scope, scopeId, endpoint, maxRequests, windowSeconds } = req.body;
      if (!scope || !endpoint || !maxRequests || !windowSeconds) {
        return next(new AppError(400, 'scope, endpoint, maxRequests, and windowSeconds are required'));
      }
      if (scope !== 'GLOBAL' && scope !== 'GROUP' && scope !== 'USER') {
        return next(new AppError(400, 'scope must be GLOBAL, GROUP, or USER'));
      }
      if (maxRequests <= 0) {
        return next(new AppError(400, 'maxRequests must be greater than 0'));
      }
      if (windowSeconds <= 0) {
        return next(new AppError(400, 'windowSeconds must be greater than 0'));
      }
      const limit = await repos.rateLimits.create({ scope, scopeId, endpoint, maxRequests, windowSeconds });
      res.status(201).json(limit);
    } catch (err) {
      next(err);
    }
  });

  // PUT /rate-limits/:id — update rate limit
  router.put('/rate-limits/:id', async (req, res, next) => {
    try {
      const existing = await repos.rateLimits.getById(req.params.id);
      if (!existing) {
        return next(new AppError(404, 'Rate limit not found'));
      }
      const updated = await repos.rateLimits.update(req.params.id, req.body);
      res.json(updated);
    } catch (err) {
      next(err);
    }
  });

  // DELETE /rate-limits/:id — delete rate limit
  router.delete('/rate-limits/:id', async (req, res, next) => {
    try {
      const deleted = await repos.rateLimits.delete(req.params.id);
      if (!deleted) {
        return next(new AppError(404, 'Rate limit not found'));
      }
      res.json({ message: 'Rate limit deleted' });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
