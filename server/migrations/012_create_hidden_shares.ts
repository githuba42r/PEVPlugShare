import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('hidden_shares', (t) => {
    t.uuid('id').primary();
    t.uuid('user_id').references('id').inTable('users').notNullable();
    t.uuid('location_id').references('id').inTable('server_locations').notNullable();
    t.uuid('share_id').references('id').inTable('location_shares').notNullable();
    t.timestamp('hidden_at').defaultTo(knex.fn.now());
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('hidden_shares');
}
