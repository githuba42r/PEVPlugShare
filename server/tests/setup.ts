import knex, { type Knex } from 'knex';

let db: Knex | null = null;

export async function setupTestDb(): Promise<Knex> {
  db = knex({
    client: 'better-sqlite3',
    connection: { filename: ':memory:' },
    useNullAsDefault: true,
    migrations: {
      directory: './migrations',
      extension: 'ts',
      loadExtensions: ['.ts'],
    },
  });

  await db.migrate.latest();
  return db;
}

export async function teardownTestDb(): Promise<void> {
  if (db) {
    await db.destroy();
    db = null;
  }
}

export function getTestDb(): Knex {
  if (!db) {
    throw new Error('Test database not initialized. Call setupTestDb() first.');
  }
  return db;
}
