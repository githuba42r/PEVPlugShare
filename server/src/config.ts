import dotenv from 'dotenv';

dotenv.config();

export interface Config {
  port: number;
  dbBackend: 'postgres' | 'mysql';
  databaseUrl: string;
  redisUrl: string | undefined;
  googleClientId: string;
  jwtSecret: string;
  storagePath: string;
  serverBaseUrl: string;
  logLevel: string;
  maxUploadSizeMb: number;
  adminSetupEmail: string | undefined;
  sessionSecret: string;
}

function getDbBackend(value: string | undefined): 'postgres' | 'mysql' {
  const backend = value ?? 'postgres';
  if (backend !== 'postgres' && backend !== 'mysql') {
    throw new Error(
      `Invalid DB_BACKEND value: "${backend}". Must be "postgres" or "mysql".`
    );
  }
  return backend;
}

export const config: Config = {
  port: parseInt(process.env['PORT'] ?? '8080', 10),
  dbBackend: getDbBackend(process.env['DB_BACKEND']),
  databaseUrl: process.env['DATABASE_URL'] ?? '',
  redisUrl: process.env['REDIS_URL'],
  googleClientId: process.env['GOOGLE_CLIENT_ID'] ?? '',
  jwtSecret: process.env['JWT_SECRET'] ?? '',
  storagePath: process.env['STORAGE_PATH'] ?? './data/photos',
  serverBaseUrl: process.env['SERVER_BASE_URL'] ?? 'http://localhost:8080',
  logLevel: process.env['LOG_LEVEL'] ?? 'info',
  maxUploadSizeMb: parseInt(process.env['MAX_UPLOAD_SIZE_MB'] ?? '5', 10),
  adminSetupEmail: process.env['ADMIN_SETUP_EMAIL'],
  sessionSecret: process.env['SESSION_SECRET'] || process.env['JWT_SECRET'] || 'dev-session-secret',
};
