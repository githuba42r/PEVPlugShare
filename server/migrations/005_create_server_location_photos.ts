import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('server_location_photos', (t) => {
    t.uuid('id').primary();
    t.uuid('location_id')
      .references('id')
      .inTable('server_locations')
      .onDelete('CASCADE')
      .notNullable();
    t.string('file_path').notNullable();
    t.string('thumbnail_path').nullable();
    t.string('caption').nullable();
    t.double('latitude').nullable();
    t.double('longitude').nullable();
    t.double('altitude').nullable();
    t.float('bearing').nullable();
    t.bigInteger('timestamp').nullable();
    t.text('exif_data').nullable();
    t.integer('width').nullable();
    t.integer('height').nullable();
    t.bigInteger('file_size').nullable();
    t.string('mime_type').defaultTo('image/jpeg');
    t.timestamp('uploaded_at').defaultTo(knex.fn.now());
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable('server_location_photos');
}
