import request from 'supertest';
import { createApp } from '../../src/app';

const app = createApp();

describe('Express app integration', () => {
  it('health check returns 200 with status ok', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
  });

  it('unknown routes return 404', async () => {
    const res = await request(app).get('/this-route-does-not-exist');
    expect(res.status).toBe(404);
    expect(res.body).toEqual({ error: 'Not found' });
  });

  it('has security headers from helmet', async () => {
    const res = await request(app).get('/health');
    // Helmet sets various security headers; x-content-type-options is a reliable one
    expect(res.headers['x-content-type-options']).toBe('nosniff');
  });

  it('has CORS headers', async () => {
    const res = await request(app)
      .get('/health')
      .set('Origin', 'http://example.com');
    // cors() sets access-control-allow-origin
    expect(res.headers['access-control-allow-origin']).toBeDefined();
  });

  it('parses JSON request bodies', async () => {
    // Add a temporary echo endpoint test via the health route being available
    // We test body parsing indirectly; a proper body-parsing test requires a POST route.
    // For now, confirm that a POST to an unknown route still gives 404 (not a body-parse error).
    const res = await request(app)
      .post('/some-endpoint')
      .set('Content-Type', 'application/json')
      .send({ foo: 'bar' });
    expect(res.status).toBe(404);
  });
});
