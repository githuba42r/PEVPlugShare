import { signToken, verifyToken, JwtPayload } from '../../src/auth/jwt';

const TEST_SECRET = 'test-secret-key';

const basePayload: JwtPayload = {
  userId: 'user-123',
  email: 'test@example.com',
  isAdmin: false,
  hasSharingBan: false,
};

describe('JWT sign/verify roundtrip', () => {
  it('signs a token and verifies it successfully', () => {
    const token = signToken(basePayload, TEST_SECRET);
    const result = verifyToken(token, TEST_SECRET);

    expect(result).not.toBeNull();
    expect(result!.userId).toBe('user-123');
    expect(result!.email).toBe('test@example.com');
    expect(result!.isAdmin).toBe(false);
    expect(result!.hasSharingBan).toBe(false);
  });

  it('returns null for an invalid token', () => {
    const result = verifyToken('not.a.valid.token', TEST_SECRET);
    expect(result).toBeNull();
  });

  it('returns null for a token signed with the wrong secret', () => {
    const token = signToken(basePayload, TEST_SECRET);
    const result = verifyToken(token, 'wrong-secret');
    expect(result).toBeNull();
  });

  it('returns null for an expired token', () => {
    const token = signToken(basePayload, TEST_SECRET, '0s');
    // Small delay to ensure the token is expired
    return new Promise<void>((resolve) => {
      setTimeout(() => {
        const result = verifyToken(token, TEST_SECRET);
        expect(result).toBeNull();
        resolve();
      }, 10);
    });
  });

  it('includes isAdmin claim correctly when true', () => {
    const adminPayload: JwtPayload = { ...basePayload, isAdmin: true };
    const token = signToken(adminPayload, TEST_SECRET);
    const result = verifyToken(token, TEST_SECRET);

    expect(result).not.toBeNull();
    expect(result!.isAdmin).toBe(true);
  });

  it('includes hasSharingBan claim correctly when true', () => {
    const bannedPayload: JwtPayload = { ...basePayload, hasSharingBan: true };
    const token = signToken(bannedPayload, TEST_SECRET);
    const result = verifyToken(token, TEST_SECRET);

    expect(result).not.toBeNull();
    expect(result!.hasSharingBan).toBe(true);
  });

  it('includes both admin and sharing ban claims correctly', () => {
    const fullPayload: JwtPayload = {
      userId: 'admin-456',
      email: 'admin@example.com',
      isAdmin: true,
      hasSharingBan: true,
    };
    const token = signToken(fullPayload, TEST_SECRET);
    const result = verifyToken(token, TEST_SECRET);

    expect(result).not.toBeNull();
    expect(result!.userId).toBe('admin-456');
    expect(result!.email).toBe('admin@example.com');
    expect(result!.isAdmin).toBe(true);
    expect(result!.hasSharingBan).toBe(true);
  });
});
