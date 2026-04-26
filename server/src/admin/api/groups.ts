import { Router } from 'express';
import type { Repositories } from '../../repositories/factory';

export function createAdminGroupRouter(repos: Repositories): Router {
  const router = Router();

  // GET /groups
  router.get('/groups', async (req, res, next) => {
    try {
      const cursor = req.query['cursor'] as string | undefined;
      const limitRaw = req.query['limit'];
      const limit = limitRaw ? parseInt(limitRaw as string, 10) : undefined;
      const result = await repos.groups.listAll(cursor, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  // GET /groups/:id
  router.get('/groups/:id', async (req, res, next) => {
    try {
      const group = await repos.groups.getById(req.params['id']!);
      if (!group) {
        res.status(404).json({ error: 'Group not found' });
        return;
      }
      const members = await repos.groups.getMembers(req.params['id']!);
      res.json({ ...group, members });
    } catch (err) {
      next(err);
    }
  });

  // DELETE /groups/:id
  router.delete('/groups/:id', async (req, res, next) => {
    try {
      const deleted = await repos.groups.delete(req.params['id']!);
      if (!deleted) {
        res.status(404).json({ error: 'Group not found' });
        return;
      }
      res.json({ deleted });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
