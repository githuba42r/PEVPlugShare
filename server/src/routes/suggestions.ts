import { Router } from 'express';
import type { SuggestionService } from '../services/suggestion-service';
import type { SuggestionRepository } from '../repositories/interfaces';

export function createSuggestionRouter(
  suggestionService: SuggestionService,
  suggestionRepo: SuggestionRepository,
): Router {
  const router = Router();

  // POST /api/locations/:id/suggestions — submit a suggestion
  router.post('/api/locations/:id/suggestions', async (req, res, next) => {
    try {
      const locationId = req.params['id']!;
      const userId = req.userId!;
      const { changesJson, message } = req.body as {
        changesJson?: string;
        message?: string;
      };

      if (!changesJson) {
        res.status(400).json({ error: 'changesJson is required' });
        return;
      }

      const suggestion = await suggestionService.submit(locationId, userId, changesJson, message);
      res.status(201).json(suggestion);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/suggestions/incoming — MUST be before /:id routes
  router.get('/api/suggestions/incoming', async (req, res, next) => {
    try {
      const userId = req.userId!;
      const cursor = req.query['cursor'] as string | undefined;
      const limitRaw = req.query['limit'];
      const limit = limitRaw ? parseInt(limitRaw as string, 10) : undefined;

      const result = await suggestionRepo.getIncoming(userId, cursor, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/suggestions/outgoing — MUST be before /:id routes
  router.get('/api/suggestions/outgoing', async (req, res, next) => {
    try {
      const userId = req.userId!;
      const cursor = req.query['cursor'] as string | undefined;
      const limitRaw = req.query['limit'];
      const limit = limitRaw ? parseInt(limitRaw as string, 10) : undefined;

      const result = await suggestionRepo.getOutgoing(userId, cursor, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  // PUT /api/suggestions/:id/accept
  router.put('/api/suggestions/:id/accept', async (req, res, next) => {
    try {
      const suggestionId = req.params['id']!;
      const userId = req.userId!;
      const { responseMsg } = req.body as { responseMsg?: string };

      const suggestion = await suggestionService.accept(suggestionId, userId, responseMsg);
      res.json(suggestion);
    } catch (err) {
      next(err);
    }
  });

  // PUT /api/suggestions/:id/reject
  router.put('/api/suggestions/:id/reject', async (req, res, next) => {
    try {
      const suggestionId = req.params['id']!;
      const userId = req.userId!;
      const { responseMsg } = req.body as { responseMsg?: string };

      const suggestion = await suggestionService.reject(suggestionId, userId, responseMsg);
      res.json(suggestion);
    } catch (err) {
      next(err);
    }
  });

  // PUT /api/suggestions/:id/withdraw
  router.put('/api/suggestions/:id/withdraw', async (req, res, next) => {
    try {
      const suggestionId = req.params['id']!;
      const userId = req.userId!;

      const suggestion = await suggestionService.withdraw(suggestionId, userId);
      res.json(suggestion);
    } catch (err) {
      next(err);
    }
  });

  return router;
}
