describe('config', () => {
  const ORIGINAL_ENV = process.env;

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...ORIGINAL_ENV };
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  function loadConfig() {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    return require('../src/config') as typeof import('../src/config');
  }

  describe('defaults', () => {
    it('uses default port 8080 when PORT is not set', () => {
      delete process.env['PORT'];
      const { config } = loadConfig();
      expect(config.port).toBe(8080);
    });

    it('uses default dbBackend "postgres" when DB_BACKEND is not set', () => {
      delete process.env['DB_BACKEND'];
      const { config } = loadConfig();
      expect(config.dbBackend).toBe('postgres');
    });

    it('uses default storagePath when STORAGE_PATH is not set', () => {
      delete process.env['STORAGE_PATH'];
      const { config } = loadConfig();
      expect(config.storagePath).toBe('./data/photos');
    });

    it('uses default serverBaseUrl when SERVER_BASE_URL is not set', () => {
      delete process.env['SERVER_BASE_URL'];
      const { config } = loadConfig();
      expect(config.serverBaseUrl).toBe('http://localhost:8080');
    });

    it('uses default logLevel "info" when LOG_LEVEL is not set', () => {
      delete process.env['LOG_LEVEL'];
      const { config } = loadConfig();
      expect(config.logLevel).toBe('info');
    });

    it('uses default maxUploadSizeMb 5 when MAX_UPLOAD_SIZE_MB is not set', () => {
      delete process.env['MAX_UPLOAD_SIZE_MB'];
      const { config } = loadConfig();
      expect(config.maxUploadSizeMb).toBe(5);
    });

    it('sets adminSetupEmail to undefined when ADMIN_SETUP_EMAIL is not set', () => {
      delete process.env['ADMIN_SETUP_EMAIL'];
      const { config } = loadConfig();
      expect(config.adminSetupEmail).toBeUndefined();
    });

    it('sets redisUrl to undefined when REDIS_URL is not set', () => {
      delete process.env['REDIS_URL'];
      const { config } = loadConfig();
      expect(config.redisUrl).toBeUndefined();
    });
  });

  describe('reads values from environment', () => {
    it('reads PORT from environment', () => {
      process.env['PORT'] = '3000';
      const { config } = loadConfig();
      expect(config.port).toBe(3000);
    });

    it('reads DB_BACKEND postgres from environment', () => {
      process.env['DB_BACKEND'] = 'postgres';
      const { config } = loadConfig();
      expect(config.dbBackend).toBe('postgres');
    });

    it('reads DB_BACKEND mysql from environment', () => {
      process.env['DB_BACKEND'] = 'mysql';
      const { config } = loadConfig();
      expect(config.dbBackend).toBe('mysql');
    });

    it('reads DATABASE_URL from environment', () => {
      process.env['DATABASE_URL'] = 'postgresql://user:pass@host:5432/db';
      const { config } = loadConfig();
      expect(config.databaseUrl).toBe('postgresql://user:pass@host:5432/db');
    });

    it('reads REDIS_URL from environment', () => {
      process.env['REDIS_URL'] = 'redis://localhost:6379';
      const { config } = loadConfig();
      expect(config.redisUrl).toBe('redis://localhost:6379');
    });

    it('reads GOOGLE_CLIENT_ID from environment', () => {
      process.env['GOOGLE_CLIENT_ID'] = 'test-client-id.apps.googleusercontent.com';
      const { config } = loadConfig();
      expect(config.googleClientId).toBe('test-client-id.apps.googleusercontent.com');
    });

    it('reads JWT_SECRET from environment', () => {
      process.env['JWT_SECRET'] = 'super-secret-key';
      const { config } = loadConfig();
      expect(config.jwtSecret).toBe('super-secret-key');
    });

    it('reads STORAGE_PATH from environment', () => {
      process.env['STORAGE_PATH'] = '/var/data/photos';
      const { config } = loadConfig();
      expect(config.storagePath).toBe('/var/data/photos');
    });

    it('reads SERVER_BASE_URL from environment', () => {
      process.env['SERVER_BASE_URL'] = 'https://api.pevplugshare.com';
      const { config } = loadConfig();
      expect(config.serverBaseUrl).toBe('https://api.pevplugshare.com');
    });

    it('reads LOG_LEVEL from environment', () => {
      process.env['LOG_LEVEL'] = 'debug';
      const { config } = loadConfig();
      expect(config.logLevel).toBe('debug');
    });

    it('reads MAX_UPLOAD_SIZE_MB from environment', () => {
      process.env['MAX_UPLOAD_SIZE_MB'] = '10';
      const { config } = loadConfig();
      expect(config.maxUploadSizeMb).toBe(10);
    });

    it('reads ADMIN_SETUP_EMAIL from environment', () => {
      process.env['ADMIN_SETUP_EMAIL'] = 'admin@example.com';
      const { config } = loadConfig();
      expect(config.adminSetupEmail).toBe('admin@example.com');
    });
  });

  describe('validation', () => {
    it('throws for invalid DB_BACKEND value', () => {
      process.env['DB_BACKEND'] = 'sqlite';
      expect(() => loadConfig()).toThrow(
        /Invalid DB_BACKEND value.*sqlite.*Must be "postgres" or "mysql"/
      );
    });

    it('throws for empty string DB_BACKEND', () => {
      process.env['DB_BACKEND'] = '';
      expect(() => loadConfig()).toThrow(/Invalid DB_BACKEND/);
    });

    it('throws for mixed-case DB_BACKEND', () => {
      process.env['DB_BACKEND'] = 'Postgres';
      expect(() => loadConfig()).toThrow(/Invalid DB_BACKEND/);
    });
  });
});
