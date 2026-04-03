import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('group_invitations', (t) => {
    t.uuid('id').primary();
    t.uuid('group_id')
      .references('id')
      .inTable('friend_groups')
      .onDelete('CASCADE')
      .notNullable();
    t.uuid('invited_by').references('id').inTable('users').notNullable();
    t.uuid('invitee_id').references('id').inTable('users').nullable();
    t.string('invitee_email').nullable();
    t.string('token').unique().notNullable();
    t.string('status').notNullable().defaultTo('PENDING');
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.timestamp('expires_at').nullable();
    t.timestamp('responded_at').nullable();
    t.uuid('responded_by').references('id').inTable('users').nullable();
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('group_invitations');
}
