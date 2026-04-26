import type { Knex } from 'knex';
import { v4 as uuidv4 } from 'uuid';

export async function up(knex: Knex): Promise<void> {
  await knex('rate_limits').insert([
    {
      id: uuidv4(),
      scope: 'GLOBAL',
      scope_id: null,
      endpoint: 'POST /api/locations',
      max_requests: 100,
      window_seconds: 3600,
    },
    {
      id: uuidv4(),
      scope: 'GLOBAL',
      scope_id: null,
      endpoint: 'POST /api/locations/*/photos',
      max_requests: 200,
      window_seconds: 3600,
    },
    {
      id: uuidv4(),
      scope: 'GLOBAL',
      scope_id: null,
      endpoint: 'POST /api/friends/request',
      max_requests: 50,
      window_seconds: 3600,
    },
    {
      id: uuidv4(),
      scope: 'GLOBAL',
      scope_id: null,
      endpoint: '*',
      max_requests: 1000,
      window_seconds: 3600,
    },
  ]);
}

export async function down(knex: Knex): Promise<void> {
  await knex('rate_limits')
    .where('scope', 'GLOBAL')
    .whereNull('scope_id')
    .whereIn('endpoint', [
      'POST /api/locations',
      'POST /api/locations/*/photos',
      'POST /api/friends/request',
      '*',
    ])
    .delete();
}
