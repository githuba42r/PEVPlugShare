import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('location_shares', (t) => {
    t.uuid('id').primary();
    t.uuid('location_id').references('id').inTable('server_locations').notNullable();
    t.uuid('shared_by').references('id').inTable('users').notNullable();
    t.uuid('shared_with_user').references('id').inTable('users').nullable();
    t.uuid('shared_with_group').references('id').inTable('friend_groups').nullable();
    t.boolean('is_active').defaultTo(true);
    t.timestamp('shared_at').defaultTo(knex.fn.now());
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('location_shares');
}
