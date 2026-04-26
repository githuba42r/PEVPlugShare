import type { Knex } from 'knex';

const sharedMigrationConfig = {
  directory: './migrations',
  extension: 'ts',
};

const configs: Record<string, Knex.Config> = {
  postgres: {
    client: 'pg',
    connection: process.env.DATABASE_URL,
    migrations: sharedMigrationConfig,
  },
  mysql: {
    client: 'mysql2',
    connection: process.env.DATABASE_URL,
    migrations: sharedMigrationConfig,
  },
  test: {
    client: 'better-sqlite3',
    connection: { filename: ':memory:' },
    useNullAsDefault: true,
    migrations: sharedMigrationConfig,
  },
};

function getConfig(): Knex.Config {
  if (process.env.NODE_ENV === 'test') {
    return configs.test;
  }
  const backend = process.env.DB_BACKEND ?? 'postgres';
  return configs[backend] ?? configs.postgres;
}

const config = getConfig();

export default config;
module.exports = config;
