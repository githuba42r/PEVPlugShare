import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('banned_identifiers', (t) => {
    t.uuid('id').primary();
    t.string('identifier_type').notNullable();
    t.string('identifier').notNullable();
    t.string('reason').nullable();
    t.uuid('banned_by').references('id').inTable('users').nullable();
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.unique(['identifier_type', 'identifier']);
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('banned_identifiers');
}
