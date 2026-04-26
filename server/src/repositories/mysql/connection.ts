import knex, { type Knex } from 'knex';

export function createMysqlConnection(url: string): Knex {
  return knex({
    client: 'mysql2',
    connection: url,
  });
}
