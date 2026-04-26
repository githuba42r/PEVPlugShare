import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('server_locations', (t) => {
    t.uuid('id').primary();
    t.uuid('owner_id').references('id').inTable('users').notNullable();
    t.string('name').notNullable();
    t.double('latitude').notNullable();
    t.double('longitude').notNullable();
    t.text('description').nullable();
    t.text('additional_info').nullable();
    t.string('opening_hours').nullable();
    t.string('socket_types').nullable();
    t.integer('gpo_count').nullable();
    t.boolean('has_fee').nullable();
    t.string('charging_costs').nullable();
    t.string('public_private').nullable();
    t.string('provider_name').nullable();
    t.string('provider_website').nullable();
    t.string('business_name').nullable();
    t.string('street_address').nullable();
    t.string('suburb').nullable();
    t.string('state').nullable();
    t.string('postcode').nullable();
    t.string('color').nullable();
    t.string('icon').nullable();
    t.boolean('is_deleted').defaultTo(false);
    t.string('deleted_reason').nullable();
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.timestamp('updated_at').defaultTo(knex.fn.now());
    t.integer('version').defaultTo(1);
    t.index(['latitude', 'longitude']);
    t.index('owner_id');
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('server_locations');
}
