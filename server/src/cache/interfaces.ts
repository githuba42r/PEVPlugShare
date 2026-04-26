export interface Cache {
  get(key: string): Promise<string | null>;
  set(key: string, value: string, ttlSeconds?: number): Promise<void>;
  del(key: string): Promise<void>;
  increment(key: string, ttlSeconds?: number): Promise<number>;
  exists(key: string): Promise<boolean>;
}
