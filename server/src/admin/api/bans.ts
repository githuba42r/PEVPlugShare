import { Router } from 'express';
import type { Repositories } from '../../repositories/factory';
import { AppError } from '../../middleware/error-handler';

export function createAdminBanRouter(repos: Repositories): Router {
  const router = Router();

  // GET /bans — list all banned identifiers
  router.get('/bans', async (_req, res, next) => {
    try {
      const bans = await repos.bans.listBannedIdentifiers();
      res.json(bans);
    } catch (err) {
      next(err);
    }
  });

  // POST /bans — add a banned identifier
  router.post('/bans', async (req: any, res, next) => {
    try {
      const { type, identifier, reason } = req.body;
      if (!type || !identifier) {
        return next(new AppError(400, 'type and identifier are required'));
      }
      if (type !== 'EMAIL' && type !== 'GOOGLE_ID') {
        return next(new AppError(400, 'type must be EMAIL or GOOGLE_ID'));
      }
      const adminUserId: string = (req.session as any)?.userId ?? req.userId ?? 'admin';
      const ban = await repos.bans.addBannedIdentifier(type, identifier, reason, adminUserId);
      res.status(201).json(ban);
    } catch (err) {
      next(err);
    }
  });

  // DELETE /bans/:id — remove a banned identifier
  router.delete('/bans/:id', async (req, res, next) => {
    try {
      const removed = await repos.bans.removeBannedIdentifier(req.params.id);
      if (!removed) {
        return next(new AppError(404, 'Ban not found'));
      }
      res.json({ message: 'Ban removed' });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
