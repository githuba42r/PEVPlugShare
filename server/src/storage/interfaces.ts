export interface FileStorage {
  save(relativePath: string, data: Buffer): Promise<void>;
  read(relativePath: string): Promise<Buffer>;
  delete(relativePath: string): Promise<void>;
  exists(relativePath: string): Promise<boolean>;
  getAbsolutePath(relativePath: string): string;
}
