import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('friendships', (t) => {
    t.uuid('id').primary();
    t.uuid('requester_id').references('id').inTable('users').notNullable();
    t.uuid('addressee_id').references('id').inTable('users').notNullable();
    t.string('status').notNullable();
    t.timestamp('requested_at').defaultTo(knex.fn.now());
    t.timestamp('responded_at').nullable();
    t.unique(['requester_id', 'addressee_id']);
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('friendships');
}
