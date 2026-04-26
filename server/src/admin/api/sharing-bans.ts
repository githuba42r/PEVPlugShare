import { Router } from 'express';
import type { Repositories } from '../../repositories/factory';
import { AppError } from '../../middleware/error-handler';

export function createAdminSharingBanRouter(repos: Repositories): Router {
  const router = Router();

  // GET /sharing-bans — list all sharing bans
  router.get('/sharing-bans', async (_req, res, next) => {
    try {
      const bans = await repos.bans.listSharingBans();
      res.json(bans);
    } catch (err) {
      next(err);
    }
  });

  // POST /users/:id/sharing-ban — add sharing ban for user
  router.post('/users/:id/sharing-ban', async (req: any, res, next) => {
    try {
      const { reason, expiresAt: expiresAtRaw } = req.body;
      if (!reason || typeof reason !== 'string') {
        return next(new AppError(400, 'reason is required'));
      }
      const adminUserId: string = (req.session as any)?.userId ?? req.userId ?? 'admin';
      const expiresAt = expiresAtRaw ? new Date(expiresAtRaw) : undefined;
      const ban = await repos.bans.addSharingBan(req.params.id, reason, adminUserId, expiresAt);
      res.status(201).json(ban);
    } catch (err) {
      next(err);
    }
  });

  // DELETE /users/:id/sharing-ban — remove active sharing ban for user
  router.delete('/users/:id/sharing-ban', async (req, res, next) => {
    try {
      const hasActive = await repos.bans.hasActiveSharingBan(req.params.id);
      if (!hasActive) {
        return next(new AppError(404, 'No active sharing ban found for user'));
      }
      // Find and remove the matching ban
      const allBans = await repos.bans.listSharingBans();
      const ban = allBans.find((b) => b.bannedUserId === req.params.id);
      if (!ban) {
        return next(new AppError(404, 'No active sharing ban found for user'));
      }
      await repos.bans.removeSharingBan(ban.id);
      res.json({ message: 'Sharing ban removed' });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
