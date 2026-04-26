import knex from 'knex';
import { createApp } from './app';
import { config } from './config';
import { createRepositories } from './repositories/factory';
import { LocalStorage } from './storage/local-storage';
import { MemoryCache } from './cache/memory-cache';
import { RedisCache } from './cache/redis-cache';

async function main() {
  const db = knex({
    client: config.dbBackend === 'mysql' ? 'mysql2' : 'pg',
    connection: config.databaseUrl,
  });

  // Run migrations
  await db.migrate.latest({ directory: './migrations', extension: 'ts' });
  console.log('Database migrations applied');

  const repos = createRepositories(db);
  const storage = new LocalStorage(config.storagePath);

  let cache;
  if (config.redisUrl) {
    const redisCache = new RedisCache(config.redisUrl);
    await redisCache.connect();
    cache = redisCache;
    console.log('Redis cache connected');
  } else {
    cache = new MemoryCache();
    console.log('Using in-memory cache');
  }

  const app = createApp({ repos, storage, cache });
  app.listen(config.port, () => {
    console.log(`PEVPlugShare server listening on port ${config.port}`);
    console.log(`Database backend: ${config.dbBackend}`);
  });
}

main().catch((err) => {
  console.error('Failed to start server:', err);
  process.exit(1);
});
