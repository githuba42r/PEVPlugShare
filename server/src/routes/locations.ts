import { Router } from 'express';
import type { Repositories } from '../repositories/factory';
import { AppError } from '../middleware/error-handler';

export function createLocationRouter(repos: Repositories): Router {
  const router = Router();

  // POST /api/locations
  router.post('/api/locations', async (req, res, next) => {
    try {
      const { name, latitude, longitude } = req.body as {
        name?: string;
        latitude?: number;
        longitude?: number;
      };

      if (!name || latitude === undefined || longitude === undefined) {
        throw new AppError(400, 'name, latitude, and longitude are required');
      }

      const location = await repos.locations.create({
        ...req.body,
        ownerId: req.userId!,
      });

      res.status(201).json(location);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/locations — list own locations
  router.get('/api/locations', async (req, res, next) => {
    try {
      const cursor = req.query['cursor'] as string | undefined;
      const limitRaw = req.query['limit'];
      const limit = limitRaw ? parseInt(limitRaw as string, 10) : undefined;

      const result = await repos.locations.getByOwner(req.userId!, cursor, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/locations/shared-with-me — MUST be before /:id
  router.get('/api/locations/shared-with-me', (_req, res) => {
    res.json({ data: [], pagination: { cursor: null, hasMore: false } });
  });

  // GET /api/locations/nearby — MUST be before /:id
  router.get('/api/locations/nearby', async (req, res, next) => {
    try {
      const { lat, lon, radius } = req.query as {
        lat?: string;
        lon?: string;
        radius?: string;
      };

      if (!lat || !lon || !radius) {
        throw new AppError(400, 'lat, lon, and radius are required');
      }

      const cursor = req.query['cursor'] as string | undefined;
      const limitRaw = req.query['limit'];
      const limit = limitRaw ? parseInt(limitRaw as string, 10) : undefined;

      const result = await repos.locations.getNearby(
        parseFloat(lat),
        parseFloat(lon),
        parseFloat(radius),
        cursor,
        limit,
      );

      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/locations/:id
  router.get('/api/locations/:id', async (req, res, next) => {
    try {
      const location = await repos.locations.getById(req.params['id']!);
      if (!location || location.isDeleted) {
        throw new AppError(404, 'Location not found');
      }

      if (location.ownerId !== req.userId) {
        throw new AppError(403, 'Forbidden');
      }

      res.json(location);
    } catch (err) {
      next(err);
    }
  });

  // PUT /api/locations/:id
  router.put('/api/locations/:id', async (req, res, next) => {
    try {
      const location = await repos.locations.getById(req.params['id']!);
      if (!location || location.isDeleted) {
        throw new AppError(404, 'Location not found');
      }

      if (location.ownerId !== req.userId) {
        throw new AppError(403, 'Forbidden');
      }

      const updated = await repos.locations.update(req.params['id']!, req.body);
      res.json(updated);
    } catch (err) {
      next(err);
    }
  });

  // DELETE /api/locations/:id
  router.delete('/api/locations/:id', async (req, res, next) => {
    try {
      const { reason } = req.body as { reason?: string };

      const location = await repos.locations.getById(req.params['id']!);
      if (!location || location.isDeleted) {
        throw new AppError(404, 'Location not found');
      }

      if (location.ownerId !== req.userId) {
        throw new AppError(403, 'Forbidden');
      }

      await repos.locations.softDelete(req.params['id']!, reason);
      res.json({ message: 'Location deleted' });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
