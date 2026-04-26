import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('rate_limits', (t) => {
    t.uuid('id').primary();
    t.string('scope').notNullable();
    t.uuid('scope_id').nullable();
    t.string('endpoint').notNullable();
    t.integer('max_requests').notNullable();
    t.integer('window_seconds').notNullable();
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.timestamp('updated_at').defaultTo(knex.fn.now());
    t.unique(['scope', 'scope_id', 'endpoint']);
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('rate_limits');
}
