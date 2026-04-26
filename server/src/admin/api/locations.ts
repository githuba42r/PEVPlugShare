import { Router } from 'express';
import type { Repositories } from '../../repositories/factory';
import { AppError } from '../../middleware/error-handler';

export function createAdminLocationRouter(repos: Repositories): Router {
  const router = Router();

  // GET /locations — list all locations (with optional pagination)
  router.get('/locations', async (req, res, next) => {
    try {
      const cursor = req.query.cursor as string | undefined;
      const limit = req.query.limit ? parseInt(req.query.limit as string, 10) : undefined;
      const result = await repos.locations.listAll(cursor, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  // GET /locations/:id — get location detail with photos
  router.get('/locations/:id', async (req, res, next) => {
    try {
      const location = await repos.locations.getById(req.params.id);
      if (!location) {
        return next(new AppError(404, 'Location not found'));
      }
      const photos = await repos.photos.getByLocationId(req.params.id);
      res.json({ ...location, photos });
    } catch (err) {
      next(err);
    }
  });

  // DELETE /locations/:id — soft-delete location
  router.delete('/locations/:id', async (req, res, next) => {
    try {
      const { reason } = req.body;
      const deleted = await repos.locations.softDelete(req.params.id, reason ?? 'Deleted by admin');
      if (!deleted) {
        return next(new AppError(404, 'Location not found'));
      }
      res.json({ message: 'Location deleted' });
    } catch (err) {
      next(err);
    }
  });

  // PUT /locations/:id/block-sharing — block sharing (soft-delete with reason)
  router.put('/locations/:id/block-sharing', async (req, res, next) => {
    try {
      const deleted = await repos.locations.softDelete(req.params.id, 'Sharing blocked by admin');
      if (!deleted) {
        return next(new AppError(404, 'Location not found'));
      }
      res.json({ message: 'Sharing blocked' });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
