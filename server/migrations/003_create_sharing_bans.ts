import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('sharing_bans', (t) => {
    t.uuid('id').primary();
    t.uuid('user_id').references('id').inTable('users').notNullable();
    t.string('reason').notNullable();
    t.uuid('banned_by').references('id').inTable('users').notNullable();
    t.timestamp('expires_at').nullable();
    t.timestamp('created_at').defaultTo(knex.fn.now());
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('sharing_bans');
}
