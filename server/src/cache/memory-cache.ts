import { Cache } from './interfaces';

interface CacheEntry {
  value: string;
  expiresAt: number | null;
}

export class MemoryCache implements Cache {
  private store: Map<string, CacheEntry> = new Map();

  private isExpired(entry: CacheEntry): boolean {
    return entry.expiresAt !== null && Date.now() > entry.expiresAt;
  }

  async get(key: string): Promise<string | null> {
    const entry = this.store.get(key);
    if (!entry) return null;
    if (this.isExpired(entry)) {
      this.store.delete(key);
      return null;
    }
    return entry.value;
  }

  async set(key: string, value: string, ttlSeconds?: number): Promise<void> {
    const expiresAt = ttlSeconds != null ? Date.now() + ttlSeconds * 1000 : null;
    this.store.set(key, { value, expiresAt });
  }

  async del(key: string): Promise<void> {
    this.store.delete(key);
  }

  async increment(key: string, ttlSeconds?: number): Promise<number> {
    const current = await this.get(key);
    const newValue = (current !== null ? parseInt(current, 10) : 0) + 1;
    await this.set(key, String(newValue), ttlSeconds);
    return newValue;
  }

  async exists(key: string): Promise<boolean> {
    const entry = this.store.get(key);
    if (!entry) return false;
    if (this.isExpired(entry)) {
      this.store.delete(key);
      return false;
    }
    return true;
  }
}
