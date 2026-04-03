import express, { type Request, type Response, type NextFunction } from 'express';
import request from 'supertest';
import { AppError, errorHandler } from '../../src/middleware/error-handler';

function makeApp(thrownError: Error) {
  const app = express();

  app.get('/test', (_req: Request, _res: Response, next: NextFunction) => {
    next(thrownError);
  });

  // Express error handler must have 4 params
  app.use(errorHandler);

  return app;
}

describe('errorHandler middleware', () => {
  it('returns 500 with generic message for unexpected errors', async () => {
    const app = makeApp(new Error('Something secret'));
    const res = await request(app).get('/test');
    expect(res.status).toBe(500);
    expect(res.body).toEqual({ error: 'Internal server error' });
    // Original message must NOT be leaked
    expect(JSON.stringify(res.body)).not.toContain('secret');
  });

  it('returns correct status code for AppError (422)', async () => {
    const app = makeApp(new AppError(422, 'Unprocessable entity'));
    const res = await request(app).get('/test');
    expect(res.status).toBe(422);
    expect(res.body).toEqual({ error: 'Unprocessable entity' });
  });

  it('returns 404 for not-found AppError', async () => {
    const app = makeApp(new AppError(404, 'Not found'));
    const res = await request(app).get('/test');
    expect(res.status).toBe(404);
    expect(res.body).toEqual({ error: 'Not found' });
  });
});
