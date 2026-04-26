import type { Knex } from 'knex';
import { v4 as uuidv4 } from 'uuid';
import type { Photo } from '../../models';
import type { PhotoRepository } from '../interfaces';

interface PhotoRow {
  id: string;
  location_id: string;
  file_path: string;
  thumbnail_path: string | null;
  caption: string | null;
  latitude: number | null;
  longitude: number | null;
  altitude: number | null;
  bearing: number | null;
  timestamp: number | null;
  exif_data: string | null;
  width: number | null;
  height: number | null;
  file_size: number | null;
  mime_type: string | null;
  uploaded_at: Date | string;
}

function mapRow(row: PhotoRow): Photo {
  let exifData: Record<string, unknown> | null = null;
  if (row.exif_data) {
    try {
      exifData = JSON.parse(row.exif_data) as Record<string, unknown>;
    } catch {
      exifData = null;
    }
  }

  return {
    id: row.id,
    locationId: row.location_id,
    filePath: row.file_path,
    thumbnailPath: row.thumbnail_path,
    caption: row.caption,
    latitude: row.latitude,
    longitude: row.longitude,
    altitude: row.altitude,
    bearing: row.bearing,
    timestamp: row.timestamp != null ? new Date(row.timestamp) : null,
    exifData,
    width: row.width,
    height: row.height,
    fileSize: row.file_size,
    mimeType: row.mime_type,
    uploadedAt: new Date(row.uploaded_at),
  };
}

export class PostgresPhotoRepo implements PhotoRepository {
  constructor(private readonly knex: Knex) {}

  async create(locationId: string, filePath: string, thumbnailPath: string | null, metadata: Partial<Photo>): Promise<Photo> {
    const id = uuidv4();
    const now = new Date();
    await this.knex('server_location_photos').insert({
      id,
      location_id: locationId,
      file_path: filePath,
      thumbnail_path: thumbnailPath ?? null,
      caption: metadata.caption ?? null,
      latitude: metadata.latitude ?? null,
      longitude: metadata.longitude ?? null,
      altitude: metadata.altitude ?? null,
      bearing: metadata.bearing ?? null,
      timestamp: metadata.timestamp ? metadata.timestamp.getTime() : null,
      exif_data: metadata.exifData ? JSON.stringify(metadata.exifData) : null,
      width: metadata.width ?? null,
      height: metadata.height ?? null,
      file_size: metadata.fileSize ?? null,
      mime_type: metadata.mimeType ?? 'image/jpeg',
      uploaded_at: now,
    });
    const row = await this.knex('server_location_photos').where({ id }).first();
    return mapRow(row as PhotoRow);
  }

  async getById(id: string): Promise<Photo | null> {
    const row = await this.knex('server_location_photos').where({ id }).first();
    return row ? mapRow(row as PhotoRow) : null;
  }

  async getByLocationId(locationId: string): Promise<Photo[]> {
    const rows = await this.knex('server_location_photos')
      .where({ location_id: locationId })
      .orderBy('uploaded_at', 'asc');
    return rows.map((r) => mapRow(r as PhotoRow));
  }

  async delete(id: string): Promise<boolean> {
    const count = await this.knex('server_location_photos').where({ id }).delete();
    return count > 0;
  }

  async countByLocationId(locationId: string): Promise<number> {
    const result = await this.knex('server_location_photos')
      .where({ location_id: locationId })
      .count('* as count')
      .first();
    return parseInt(String((result as { count: string | number }).count), 10);
  }
}
