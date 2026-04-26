import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('users', (t) => {
    t.uuid('id').primary();
    t.string('google_id').unique().notNullable();
    t.string('email').unique().notNullable();
    t.string('display_name').notNullable();
    t.string('avatar_url').nullable();
    t.boolean('is_admin').defaultTo(false);
    t.boolean('is_banned').defaultTo(false);
    t.string('ban_reason').nullable();
    t.timestamp('banned_at').nullable();
    t.uuid('banned_by').references('id').inTable('users').nullable();
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.timestamp('last_seen_at').defaultTo(knex.fn.now());
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('users');
}
