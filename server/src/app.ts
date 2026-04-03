import express from 'express';
import path from 'path';
import helmet from 'helmet';
import cors from 'cors';
import { requestLogger } from './middleware/request-logger';
import { errorHandler } from './middleware/error-handler';
import { healthRouter } from './routes/health';
import { createAuthRouter } from './routes/auth';
import { createUserRouter } from './routes/users';
import { createLocationRouter } from './routes/locations';
import { createPhotoRouter } from './routes/photos';
import { createNotificationRouter } from './routes/notifications';
import { createAuthMiddleware } from './auth/middleware';
import { createRateLimiter } from './middleware/rate-limiter';
import { createAdminSessionRouter } from './auth/admin-session';
import { requireAdmin, requireAdminSession } from './auth/admin-middleware';
import { createAdminViewRouter } from './admin/views';
import { createAdminUserRouter } from './admin/api/users';
import { createAdminBanRouter } from './admin/api/bans';
import { createAdminSharingBanRouter } from './admin/api/sharing-bans';
import { createAdminLocationRouter } from './admin/api/locations';
import { createAdminGroupRouter } from './admin/api/groups';
import { createAdminRateLimitRouter } from './admin/api/rate-limits';
import { createAdminStatsRouter } from './admin/api/stats';
import type { Repositories } from './repositories/factory';
import type { FileStorage } from './storage/interfaces';
import type { Cache } from './cache/interfaces';
import { config } from './config';

export interface AppDependencies {
  repos: Repositories;
  storage: FileStorage;
  cache: Cache;
}

export function createApp(deps?: AppDependencies): express.Express {
  const app = express();

  // View engine
  app.set('view engine', 'ejs');
  app.set('views', path.join(__dirname, 'views'));

  app.use(helmet());
  app.use(cors());
  app.use(express.json());
  app.use(express.urlencoded({ extended: true }));
  app.use(requestLogger);

  // Health check — no auth required
  app.use(healthRouter);

  if (deps) {
    // Auth route — no JWT required
    app.use(createAuthRouter(deps.repos));

    // Admin session middleware + login/logout routes
    app.use(createAdminSessionRouter(deps.repos));

    // Admin views — session required
    app.use('/admin', requireAdminSession, createAdminViewRouter(deps.repos));

    // Admin API — admin session or JWT required
    const adminApiRouter = express.Router();
    adminApiRouter.use(requireAdmin);
    adminApiRouter.use(createAdminUserRouter(deps.repos));
    adminApiRouter.use(createAdminBanRouter(deps.repos));
    adminApiRouter.use(createAdminSharingBanRouter(deps.repos));
    adminApiRouter.use(createAdminLocationRouter(deps.repos));
    adminApiRouter.use(createAdminGroupRouter(deps.repos));
    adminApiRouter.use(createAdminRateLimitRouter(deps.repos));
    adminApiRouter.use(createAdminStatsRouter(deps.repos));
    app.use('/admin/api', adminApiRouter);

    // Apply auth middleware and rate limiter to all /api/* routes
    app.use('/api', createAuthMiddleware(config.jwtSecret));
    app.use('/api', createRateLimiter(deps.cache, deps.repos.rateLimits));

    // Mount API route handlers
    app.use(createUserRouter(deps.repos));
    app.use(createLocationRouter(deps.repos));
    app.use(createPhotoRouter(deps.repos, deps.storage));
    app.use(createNotificationRouter(deps.repos));
  }

  // 404 handler for unmatched routes
  app.use((_req, res) => {
    res.status(404).json({ error: 'Not found' });
  });

  app.use(errorHandler);

  return app;
}
