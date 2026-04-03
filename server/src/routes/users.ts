import { Router } from 'express';
import type { Repositories } from '../repositories/factory';
import { AppError } from '../middleware/error-handler';

export function createUserRouter(repos: Repositories): Router {
  const router = Router();

  // GET /api/users/me
  router.get('/api/users/me', async (req, res, next) => {
    try {
      const user = await repos.users.getById(req.userId!);
      if (!user) {
        throw new AppError(404, 'User not found');
      }

      res.json({
        id: user.id,
        email: user.email,
        displayName: user.displayName,
        avatarUrl: user.avatarUrl,
        isAdmin: user.isAdmin,
        createdAt: user.createdAt,
      });
    } catch (err) {
      next(err);
    }
  });

  // GET /api/users/search?q=query
  router.get('/api/users/search', async (req, res, next) => {
    try {
      const q = (req.query['q'] as string) ?? '';
      if (q.length < 2) {
        throw new AppError(400, 'Search query must be at least 2 characters');
      }

      const cursor = req.query['cursor'] as string | undefined;
      const limitRaw = req.query['limit'];
      const limit = limitRaw ? parseInt(limitRaw as string, 10) : undefined;

      const result = await repos.users.search(q, cursor, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  return router;
}
