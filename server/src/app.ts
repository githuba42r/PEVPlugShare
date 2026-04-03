import express from 'express';
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

  app.use(helmet());
  app.use(cors());
  app.use(express.json());
  app.use(requestLogger);

  // Health check — no auth required
  app.use(healthRouter);

  if (deps) {
    // Auth route — no JWT required
    app.use(createAuthRouter(deps.repos));

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
