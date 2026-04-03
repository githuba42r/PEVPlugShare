import { Router } from 'express';
import type { SharingService } from '../services/sharing-service';
import type { ShareRepository } from '../repositories/interfaces';

export function createSharingRouter(
  sharingService: SharingService,
  shareRepo: ShareRepository,
): Router {
  const router = Router();

  // POST /api/locations/:id/share — share with user or group
  router.post('/api/locations/:id/share', async (req, res, next) => {
    try {
      const locationId = req.params['id']!;
      const { userId, groupId } = req.body as { userId?: string; groupId?: string };
      const sharedBy = req.userId!;

      if (!userId && !groupId) {
        res.status(400).json({ error: 'Must provide userId or groupId' });
        return;
      }

      let share;
      if (userId) {
        share = await sharingService.shareWithUser(locationId, userId, sharedBy);
      } else {
        share = await sharingService.shareWithGroup(locationId, groupId!, sharedBy);
      }

      res.status(201).json(share);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/locations/:id/shares — list shares for location (paginated)
  router.get('/api/locations/:id/shares', async (req, res, next) => {
    try {
      const locationId = req.params['id']!;
      const cursor = req.query['cursor'] as string | undefined;
      const limitRaw = req.query['limit'];
      const limit = limitRaw ? parseInt(limitRaw as string, 10) : undefined;

      const result = await shareRepo.getSharesForLocation(locationId, cursor, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  // POST /api/locations/:id/share/direct — MUST be before /:id/share/:shareId
  router.post('/api/locations/:id/share/direct', async (req, res, next) => {
    try {
      const locationId = req.params['id']!;
      const userId = req.userId!;
      const { singleUse, maxUses, expiresAt } = req.body as {
        singleUse?: boolean;
        maxUses?: number;
        expiresAt?: string;
      };

      const result = await sharingService.generateDirectLink(locationId, userId, {
        singleUse,
        maxUses,
        expiresAt: expiresAt ? new Date(expiresAt) : undefined,
      });

      res.status(201).json(result);
    } catch (err) {
      next(err);
    }
  });

  // DELETE /api/locations/:id/share/:shareId — revoke share
  router.delete('/api/locations/:id/share/:shareId', async (req, res, next) => {
    try {
      const locationId = req.params['id']!;
      const shareId = req.params['shareId']!;
      const userId = req.userId!;

      const revoked = await sharingService.revokeShare(shareId, locationId, userId);
      res.json({ revoked });
    } catch (err) {
      next(err);
    }
  });

  // GET /api/share/:token — resolve direct link (NOT under /api/locations)
  router.get('/api/share/:token', async (req, res, next) => {
    try {
      const token = req.params['token']!;
      const location = await sharingService.resolveDirectLink(token);
      res.json(location);
    } catch (err) {
      next(err);
    }
  });

  // POST /api/locations/:shareId/hide — hide a share
  router.post('/api/locations/:shareId/hide', async (req, res, next) => {
    try {
      const shareId = req.params['shareId']!;
      const userId = req.userId!;
      const { locationId } = req.body as { locationId?: string };

      if (!locationId) {
        res.status(400).json({ error: 'locationId is required' });
        return;
      }

      const hidden = await sharingService.hideShare(userId, locationId, shareId);
      res.status(201).json(hidden);
    } catch (err) {
      next(err);
    }
  });

  // DELETE /api/locations/:shareId/hide — unhide a share
  router.delete('/api/locations/:shareId/hide', async (req, res, next) => {
    try {
      const shareId = req.params['shareId']!;
      const userId = req.userId!;

      const result = await sharingService.unhideShare(userId, shareId);
      res.json({ unhidden: result });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
