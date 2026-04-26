import * as fs from 'fs';
import * as path from 'path';
import { LocalStorage } from '../../src/storage/local-storage';

describe('LocalStorage', () => {
  let tempDir: string;
  let storage: LocalStorage;

  beforeEach(() => {
    tempDir = fs.mkdtempSync(path.join(require('os').tmpdir(), 'local-storage-test-'));
    storage = new LocalStorage(tempDir);
  });

  afterEach(async () => {
    await fs.promises.rm(tempDir, { recursive: true, force: true });
  });

  it('saves and reads files', async () => {
    const data = Buffer.from('hello world');
    await storage.save('test.txt', data);
    const result = await storage.read('test.txt');
    expect(result).toEqual(data);
  });

  it('checks file existence', async () => {
    expect(await storage.exists('file.txt')).toBe(false);
    await storage.save('file.txt', Buffer.from('data'));
    expect(await storage.exists('file.txt')).toBe(true);
  });

  it('deletes files', async () => {
    await storage.save('todel.txt', Buffer.from('bye'));
    await storage.delete('todel.txt');
    expect(await storage.exists('todel.txt')).toBe(false);
  });

  it('creates nested directories automatically', async () => {
    const data = Buffer.from('nested content');
    await storage.save('a/b/c/file.txt', data);
    const result = await storage.read('a/b/c/file.txt');
    expect(result).toEqual(data);
  });

  it('returns absolute path', () => {
    const absPath = storage.getAbsolutePath('some/file.txt');
    expect(absPath).toBe(path.join(tempDir, 'some/file.txt'));
    expect(path.isAbsolute(absPath)).toBe(true);
  });
});
