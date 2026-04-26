import { Router } from 'express';
import type { FriendRepository } from '../repositories/interfaces';
import type { FriendService } from '../services/friend-service';

export function createFriendRouter(
  friendService: FriendService,
  friendRepo: FriendRepository,
): Router {
  const router = Router();

  // POST /api/friends/request
  router.post('/api/friends/request', async (req, res, next) => {
    try {
      const { email, userId } = req.body as { email?: string; userId?: string };
      const friendship = await friendService.sendRequest(req.userId!, email, userId);
      res.status(201).json(friendship);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/friends/requests/incoming
  router.get('/api/friends/requests/incoming', async (req, res, next) => {
    try {
      const cursor = req.query['cursor'] as string | undefined;
      const limitRaw = req.query['limit'];
      const limit = limitRaw ? parseInt(limitRaw as string, 10) : undefined;
      const result = await friendRepo.getIncomingRequests(req.userId!, cursor, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/friends/requests/outgoing
  router.get('/api/friends/requests/outgoing', async (req, res, next) => {
    try {
      const cursor = req.query['cursor'] as string | undefined;
      const limitRaw = req.query['limit'];
      const limit = limitRaw ? parseInt(limitRaw as string, 10) : undefined;
      const result = await friendRepo.getOutgoingRequests(req.userId!, cursor, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  // PUT /api/friends/requests/:id/accept
  router.put('/api/friends/requests/:id/accept', async (req, res, next) => {
    try {
      const friendship = await friendService.acceptRequest(req.params['id']!, req.userId!);
      res.json(friendship);
    } catch (err) {
      next(err);
    }
  });

  // PUT /api/friends/requests/:id/decline
  router.put('/api/friends/requests/:id/decline', async (req, res, next) => {
    try {
      const friendship = await friendService.declineRequest(req.params['id']!, req.userId!);
      res.json(friendship);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/friends
  router.get('/api/friends', async (req, res, next) => {
    try {
      const cursor = req.query['cursor'] as string | undefined;
      const limitRaw = req.query['limit'];
      const limit = limitRaw ? parseInt(limitRaw as string, 10) : undefined;
      const result = await friendRepo.getFriends(req.userId!, cursor, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  // DELETE /api/friends/:userId
  router.delete('/api/friends/:userId', async (req, res, next) => {
    try {
      const removed = await friendService.removeFriend(req.userId!, req.params['userId']!);
      res.json({ removed });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
