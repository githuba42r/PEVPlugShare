import { Router } from 'express';
import type { Repositories } from '../repositories/factory';

export function createAdminViewRouter(repos: Repositories): Router {
  const router = Router();

  // GET / — dashboard
  router.get('/', async (req, res, next) => {
    try {
      const [users, locations, rateLimitsAll] = await Promise.all([
        repos.users.count(),
        repos.locations.count(),
        repos.rateLimits.getAll(),
      ]);
      res.render('dashboard', {
        title: 'Dashboard',
        adminEmail: (req.session as any)?.email,
        stats: { users, locations, rateLimits: rateLimitsAll.length },
      });
    } catch (err) {
      next(err);
    }
  });

  // GET /users — user list with optional search and pagination
  router.get('/users', async (req, res, next) => {
    try {
      const q = (req.query.q as string) ?? '';
      const cursor = req.query.cursor as string | undefined;
      const limit = 50;
      const result = await repos.users.search(q, cursor, limit);
      res.render('users/list', {
        title: 'Users',
        adminEmail: (req.session as any)?.email,
        users: result.data,
        nextCursor: result.pagination.hasMore ? result.pagination.cursor : null,
        q,
        cursor: cursor ?? null,
      });
    } catch (err) {
      next(err);
    }
  });

  // GET /users/:id — user detail
  router.get('/users/:id', async (req, res, next) => {
    try {
      const user = await repos.users.getById(req.params.id);
      if (!user) {
        res.status(404).render('dashboard', {
          title: 'Not Found',
          adminEmail: (req.session as any)?.email,
          stats: { users: 0, locations: 0, rateLimits: 0 },
        });
        return;
      }
      res.render('users/detail', {
        title: user.displayName,
        adminEmail: (req.session as any)?.email,
        user,
      });
    } catch (err) {
      next(err);
    }
  });

  // GET /bans — list banned identifiers
  router.get('/bans', async (req, res, next) => {
    try {
      const bans = await repos.bans.listBannedIdentifiers();
      res.render('bans/list', {
        title: 'Bans',
        adminEmail: (req.session as any)?.email,
        bans,
      });
    } catch (err) {
      next(err);
    }
  });

  // GET /sharing-bans — list sharing bans
  router.get('/sharing-bans', async (req, res, next) => {
    try {
      const sharingBans = await repos.bans.listSharingBans();
      res.render('sharing-bans/list', {
        title: 'Sharing Bans',
        adminEmail: (req.session as any)?.email,
        sharingBans,
      });
    } catch (err) {
      next(err);
    }
  });

  // GET /locations — location list with pagination
  router.get('/locations', async (req, res, next) => {
    try {
      const cursor = req.query.cursor as string | undefined;
      const limit = 50;
      const result = await repos.locations.listAll(cursor, limit);
      res.render('locations/list', {
        title: 'Locations',
        adminEmail: (req.session as any)?.email,
        locations: result.data,
        nextCursor: result.pagination.hasMore ? result.pagination.cursor : null,
        cursor: cursor ?? null,
      });
    } catch (err) {
      next(err);
    }
  });

  // GET /locations/:id — location detail
  router.get('/locations/:id', async (req, res, next) => {
    try {
      const [location, photos] = await Promise.all([
        repos.locations.getById(req.params.id),
        repos.photos.getByLocationId(req.params.id),
      ]);
      if (!location) {
        res.status(404).send('Location not found');
        return;
      }
      res.render('locations/detail', {
        title: location.name,
        adminEmail: (req.session as any)?.email,
        location,
        photos,
      });
    } catch (err) {
      next(err);
    }
  });

  // GET /groups — stub
  router.get('/groups', (_req, res) => {
    res.render('groups/list', {
      title: 'Groups',
    });
  });

  // GET /rate-limits — rate limit rules
  router.get('/rate-limits', async (req, res, next) => {
    try {
      const rateLimits = await repos.rateLimits.getAll();
      res.render('rate-limits/list', {
        title: 'Rate Limits',
        adminEmail: (req.session as any)?.email,
        rateLimits,
      });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
