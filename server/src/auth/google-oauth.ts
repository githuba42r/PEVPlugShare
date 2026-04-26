import { OAuth2Client } from 'google-auth-library';

export interface GoogleUserInfo {
  googleId: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
}

export async function verifyGoogleToken(idToken: string, clientId: string): Promise<GoogleUserInfo | null> {
  try {
    const client = new OAuth2Client(clientId);
    const ticket = await client.verifyIdToken({ idToken, audience: clientId });
    const payload = ticket.getPayload();
    if (!payload || !payload.sub || !payload.email) return null;
    return {
      googleId: payload.sub,
      email: payload.email,
      displayName: payload.name || payload.email.split('@')[0],
      avatarUrl: payload.picture || null,
    };
  } catch {
    return null;
  }
}
