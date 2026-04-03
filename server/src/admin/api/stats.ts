import { Router } from 'express';
import type { Repositories } from '../../repositories/factory';

export function createAdminStatsRouter(repos: Repositories): Router {
  const router = Router();

  // GET /stats — overall stats
  router.get('/stats', async (_req, res, next) => {
    try {
      const [users, locations, rateLimitsAll] = await Promise.all([
        repos.users.count(),
        repos.locations.count(),
        repos.rateLimits.getAll(),
      ]);
      res.json({
        users,
        locations,
        rateLimits: rateLimitsAll.length,
      });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
