import { Router } from 'express';
import type { Repositories } from '../../repositories/factory';
import { AppError } from '../../middleware/error-handler';

export function createAdminUserRouter(repos: Repositories): Router {
  const router = Router();

  // GET /users — list/search users
  router.get('/users', async (req: any, res, next) => {
    try {
      const q = (req.query.q as string) ?? '';
      const cursor = req.query.cursor as string | undefined;
      const limit = req.query.limit ? parseInt(req.query.limit as string, 10) : undefined;
      const result = await repos.users.search(q, cursor, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  // GET /users/:id — get user detail
  router.get('/users/:id', async (req, res, next) => {
    try {
      const user = await repos.users.getById(req.params.id);
      if (!user) {
        return next(new AppError(404, 'User not found'));
      }
      res.json(user);
    } catch (err) {
      next(err);
    }
  });

  // PUT /users/:id/admin — set admin status
  router.put('/users/:id/admin', async (req, res, next) => {
    try {
      const { isAdmin } = req.body;
      if (typeof isAdmin !== 'boolean') {
        return next(new AppError(400, 'isAdmin must be a boolean'));
      }
      await repos.users.setAdmin(req.params.id, isAdmin);
      res.json({ message: 'Admin status updated' });
    } catch (err) {
      next(err);
    }
  });

  // PUT /users/:id/ban — ban user
  router.put('/users/:id/ban', async (req: any, res, next) => {
    try {
      const { reason } = req.body;
      if (!reason || typeof reason !== 'string') {
        return next(new AppError(400, 'reason is required'));
      }
      const adminUserId: string = (req.session as any)?.userId ?? req.userId ?? 'admin';

      const user = await repos.users.getById(req.params.id);
      if (!user) {
        return next(new AppError(404, 'User not found'));
      }

      await repos.users.setBanned(req.params.id, true, reason, adminUserId);

      // Add email and google ID to banned identifiers
      await repos.bans.addBannedIdentifier('EMAIL', user.email, reason, adminUserId);
      await repos.bans.addBannedIdentifier('GOOGLE_ID', user.googleId, reason, adminUserId);

      res.json({ message: 'User banned' });
    } catch (err) {
      next(err);
    }
  });

  // PUT /users/:id/unban — unban user
  router.put('/users/:id/unban', async (req, res, next) => {
    try {
      await repos.users.setBanned(req.params.id, false);
      res.json({ message: 'User unbanned' });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
