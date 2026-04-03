import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('direct_share_tokens', (t) => {
    t.uuid('id').primary();
    t.uuid('location_id').references('id').inTable('server_locations').notNullable();
    t.uuid('created_by').references('id').inTable('users').notNullable();
    t.string('token').unique().notNullable();
    t.boolean('is_single_use').defaultTo(false);
    t.integer('use_count').defaultTo(0);
    t.integer('max_uses').nullable();
    t.timestamp('expires_at').nullable();
    t.timestamp('created_at').defaultTo(knex.fn.now());
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('direct_share_tokens');
}
