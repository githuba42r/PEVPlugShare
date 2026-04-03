import * as fs from 'fs/promises';
import * as path from 'path';
import { FileStorage } from './interfaces';

export class LocalStorage implements FileStorage {
  constructor(private basePath: string) {}

  getAbsolutePath(relativePath: string): string {
    return path.join(this.basePath, relativePath);
  }

  async save(relativePath: string, data: Buffer): Promise<void> {
    const absPath = this.getAbsolutePath(relativePath);
    await fs.mkdir(path.dirname(absPath), { recursive: true });
    await fs.writeFile(absPath, data);
  }

  async read(relativePath: string): Promise<Buffer> {
    const absPath = this.getAbsolutePath(relativePath);
    return fs.readFile(absPath);
  }

  async delete(relativePath: string): Promise<void> {
    const absPath = this.getAbsolutePath(relativePath);
    try {
      await fs.unlink(absPath);
    } catch (err: any) {
      if (err.code !== 'ENOENT') throw err;
    }
  }

  async exists(relativePath: string): Promise<boolean> {
    const absPath = this.getAbsolutePath(relativePath);
    try {
      await fs.access(absPath);
      return true;
    } catch {
      return false;
    }
  }
}
