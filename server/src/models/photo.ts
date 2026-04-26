export interface Photo {
  id: string;
  locationId: string;
  filePath: string;
  thumbnailPath: string | null;
  caption: string | null;
  latitude: number | null;
  longitude: number | null;
  altitude: number | null;
  bearing: number | null;
  timestamp: Date | null;
  exifData: Record<string, unknown> | null;
  width: number | null;
  height: number | null;
  fileSize: number | null;
  mimeType: string | null;
  uploadedAt: Date;
}
