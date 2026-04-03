import express from 'express';
import request from 'supertest';
import { healthRouter } from '../../src/routes/health';

const app = express();
app.use(healthRouter);

describe('GET /health', () => {
  it('returns 200 with status ok', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
  });

  it('includes a timestamp field', async () => {
    const res = await request(app).get('/health');
    expect(res.body).toHaveProperty('timestamp');
    // Should be a valid ISO date string
    expect(() => new Date(res.body.timestamp)).not.toThrow();
    expect(new Date(res.body.timestamp).toISOString()).toBe(res.body.timestamp);
  });

  it('includes an uptime field', async () => {
    const res = await request(app).get('/health');
    expect(res.body).toHaveProperty('uptime');
    expect(typeof res.body.uptime).toBe('number');
  });
});
