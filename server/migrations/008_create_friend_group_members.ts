import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('friend_group_members', (t) => {
    t.uuid('group_id')
      .references('id')
      .inTable('friend_groups')
      .onDelete('CASCADE');
    t.uuid('user_id').references('id').inTable('users');
    t.string('role').notNullable().defaultTo('MEMBER');
    t.timestamp('added_at').defaultTo(knex.fn.now());
    t.primary(['group_id', 'user_id']);
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('friend_group_members');
}
