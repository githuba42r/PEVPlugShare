import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('change_suggestions', (t) => {
    t.uuid('id').primary();
    t.uuid('location_id').references('id').inTable('server_locations').notNullable();
    t.uuid('suggested_by').references('id').inTable('users').notNullable();
    t.string('status').notNullable();
    t.text('changes_json').notNullable();
    t.string('message').nullable();
    t.string('response_msg').nullable();
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.timestamp('responded_at').nullable();
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('change_suggestions');
}
