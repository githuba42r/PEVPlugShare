import { MemoryCache } from '../../src/cache/memory-cache';

describe('MemoryCache', () => {
  let cache: MemoryCache;

  beforeEach(() => {
    cache = new MemoryCache();
  });

  it('returns null for missing keys', async () => {
    const result = await cache.get('nonexistent');
    expect(result).toBeNull();
  });

  it('stores and retrieves values', async () => {
    await cache.set('key1', 'hello');
    const result = await cache.get('key1');
    expect(result).toBe('hello');
  });

  it('deletes values', async () => {
    await cache.set('key2', 'world');
    await cache.del('key2');
    const result = await cache.get('key2');
    expect(result).toBeNull();
  });

  it('increments counters', async () => {
    const v1 = await cache.increment('counter');
    const v2 = await cache.increment('counter');
    const v3 = await cache.increment('counter');
    expect(v1).toBe(1);
    expect(v2).toBe(2);
    expect(v3).toBe(3);
  });

  it('checks existence', async () => {
    expect(await cache.exists('k')).toBe(false);
    await cache.set('k', 'v');
    expect(await cache.exists('k')).toBe(true);
  });

  it('expires values after TTL', async () => {
    await cache.set('expiring', 'value', 1);
    expect(await cache.get('expiring')).toBe('value');
    await new Promise(resolve => setTimeout(resolve, 1100));
    expect(await cache.get('expiring')).toBeNull();
  }, 5000);
});
