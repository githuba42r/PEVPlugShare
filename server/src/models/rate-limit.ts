export type RateLimitScope = 'GLOBAL' | 'GROUP' | 'USER';

export interface RateLimit {
  id: string;
  scope: RateLimitScope;
  scopeId: string | null;
  endpoint: string;
  maxRequests: number;
  windowSeconds: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface CreateRateLimitInput {
  scope: RateLimitScope;
  scopeId?: string | null;
  endpoint: string;
  maxRequests: number;
  windowSeconds: number;
}

export interface ResolvedRateLimit {
  endpoint: string;
  maxRequests: number;
  windowSeconds: number;
  scope: RateLimitScope;
  scopeId: string | null;
}
