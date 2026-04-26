import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('friend_groups', (t) => {
    t.uuid('id').primary();
    t.uuid('owner_id').references('id').inTable('users').notNullable();
    t.string('name').notNullable();
    t.string('description').nullable();
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.timestamp('updated_at').defaultTo(knex.fn.now());
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('friend_groups');
}
