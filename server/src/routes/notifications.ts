import { Router } from 'express';
import type { Repositories } from '../repositories/factory';

export function createNotificationRouter(repos: Repositories): Router {
  const router = Router();

  // GET /api/notifications
  router.get('/api/notifications', async (req, res, next) => {
    try {
      const cursor = req.query['cursor'] as string | undefined;
      const limitRaw = req.query['limit'];
      const limit = limitRaw ? parseInt(limitRaw as string, 10) : undefined;

      const result = await repos.notifications.getUnread(req.userId!, cursor, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  // PUT /api/notifications/read-all — MUST be before /:id/read
  router.put('/api/notifications/read-all', async (req, res, next) => {
    try {
      await repos.notifications.markAllRead(req.userId!);
      res.json({ message: 'All notifications marked as read' });
    } catch (err) {
      next(err);
    }
  });

  // PUT /api/notifications/:id/read
  router.put('/api/notifications/:id/read', async (req, res, next) => {
    try {
      await repos.notifications.markRead(req.params['id']!);
      res.json({ message: 'Notification marked as read' });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
