import { Router } from 'express';
import multer from 'multer';
import { v4 as uuidv4 } from 'uuid';
import type { Repositories } from '../repositories/factory';
import type { FileStorage } from '../storage/interfaces';
import { AppError } from '../middleware/error-handler';
import { config } from '../config';

export function createPhotoRouter(repos: Repositories, storage: FileStorage): Router {
  const router = Router();

  const upload = multer({
    storage: multer.memoryStorage(),
    limits: { fileSize: config.maxUploadSizeMb * 1024 * 1024 },
  });

  // POST /api/locations/:id/photos
  router.post('/api/locations/:id/photos', upload.single('photo'), async (req, res, next) => {
    try {
      const locationId = req.params['id']!;

      const location = await repos.locations.getById(locationId);
      if (!location || location.isDeleted) {
        throw new AppError(404, 'Location not found');
      }

      if (location.ownerId !== req.userId) {
        throw new AppError(403, 'Forbidden');
      }

      if (!req.file) {
        throw new AppError(400, 'Photo file is required');
      }

      const photoId = uuidv4();
      const filePath = `photos/${locationId}/${photoId}.jpg`;

      await storage.save(filePath, req.file.buffer);

      const photo = await repos.photos.create(locationId, filePath, null, {
        mimeType: req.file.mimetype || 'image/jpeg',
        fileSize: req.file.size,
      });

      res.status(201).json(photo);
    } catch (err) {
      next(err);
    }
  });

  // DELETE /api/locations/:id/photos/:photoId
  router.delete('/api/locations/:id/photos/:photoId', async (req, res, next) => {
    try {
      const locationId = req.params['id']!;
      const photoId = req.params['photoId']!;

      const location = await repos.locations.getById(locationId);
      if (!location || location.isDeleted) {
        throw new AppError(404, 'Location not found');
      }

      if (location.ownerId !== req.userId) {
        throw new AppError(403, 'Forbidden');
      }

      const photo = await repos.photos.getById(photoId);
      if (!photo) {
        throw new AppError(404, 'Photo not found');
      }

      await storage.delete(photo.filePath);
      await repos.photos.delete(photoId);

      res.json({ message: 'Photo deleted' });
    } catch (err) {
      next(err);
    }
  });

  // GET /api/photos/:photoId/image
  router.get('/api/photos/:photoId/image', async (req, res, next) => {
    try {
      const photo = await repos.photos.getById(req.params['photoId']!);
      if (!photo) {
        throw new AppError(404, 'Photo not found');
      }

      const etag = `"${photo.id}-${new Date(photo.uploadedAt).getTime()}"`;

      res.setHeader('ETag', etag);

      const ifNoneMatch = req.headers['if-none-match'];
      if (ifNoneMatch === etag) {
        res.status(304).end();
        return;
      }

      const data = await storage.read(photo.filePath);
      res.setHeader('Content-Type', photo.mimeType ?? 'image/jpeg');
      res.send(data);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/photos/:photoId/thumbnail
  router.get('/api/photos/:photoId/thumbnail', async (req, res, next) => {
    try {
      const photo = await repos.photos.getById(req.params['photoId']!);
      if (!photo) {
        throw new AppError(404, 'Photo not found');
      }

      if (!photo.thumbnailPath) {
        throw new AppError(404, 'Thumbnail not available');
      }

      const etag = `"${photo.id}-thumb-${new Date(photo.uploadedAt).getTime()}"`;

      res.setHeader('ETag', etag);

      const ifNoneMatch = req.headers['if-none-match'];
      if (ifNoneMatch === etag) {
        res.status(304).end();
        return;
      }

      const data = await storage.read(photo.thumbnailPath);
      res.setHeader('Content-Type', photo.mimeType ?? 'image/jpeg');
      res.send(data);
    } catch (err) {
      next(err);
    }
  });

  return router;
}
