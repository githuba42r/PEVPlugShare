import { Router } from 'express';
import type { Repositories } from '../../repositories/factory';

export function createAdminGroupRouter(_repos: Repositories): Router {
  const router = Router();

  // GET /groups — stub
  router.get('/groups', (_req, res) => {
    res.json({ data: [], pagination: { cursor: null, hasMore: false } });
  });

  // GET /groups/:id — stub
  router.get('/groups/:id', (_req, res) => {
    res.status(501).json({ error: 'Groups not yet implemented' });
  });

  // DELETE /groups/:id — stub
  router.delete('/groups/:id', (_req, res) => {
    res.status(501).json({ error: 'Groups not yet implemented' });
  });

  return router;
}
