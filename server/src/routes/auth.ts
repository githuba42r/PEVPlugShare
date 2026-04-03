import { Router } from 'express';
import type { Repositories } from '../repositories/factory';
import { verifyGoogleToken } from '../auth/google-oauth';
import { signToken } from '../auth/jwt';
import { config } from '../config';
import { AppError } from '../middleware/error-handler';

export function createAuthRouter(repos: Repositories): Router {
  const router = Router();

  router.post('/auth/google', async (req, res, next) => {
    try {
      const { idToken } = req.body as { idToken?: string };

      if (!idToken) {
        throw new AppError(400, 'idToken is required');
      }

      const googleUser = await verifyGoogleToken(idToken, config.googleClientId);
      if (!googleUser) {
        throw new AppError(401, 'Invalid Google token');
      }

      const { googleId, email, displayName, avatarUrl } = googleUser;

      // Check bans by identifier
      const [emailBanned, googleIdBanned] = await Promise.all([
        repos.bans.isBanned('EMAIL', email),
        repos.bans.isBanned('GOOGLE_ID', googleId),
      ]);

      if (emailBanned || googleIdBanned) {
        throw new AppError(403, 'Account is banned');
      }

      // Find or create user
      let user = await repos.users.getByGoogleId(googleId);
      if (!user) {
        user = await repos.users.create({ googleId, email, displayName, avatarUrl });

        // Auto-grant admin if adminSetupEmail matches and this is the first user
        if (config.adminSetupEmail && config.adminSetupEmail === email) {
          const userCount = await repos.users.count();
          if (userCount <= 1) {
            await repos.users.setAdmin(user.id, true);
            user = (await repos.users.getById(user.id))!;
          }
        }
      }

      // Check user-level ban
      if (user.isBanned) {
        throw new AppError(403, user.banReason ?? 'Account is banned');
      }

      await repos.users.updateLastSeen(user.id);

      const hasSharingBan = await repos.bans.hasActiveSharingBan(user.id);

      const token = signToken(
        {
          userId: user.id,
          email: user.email,
          isAdmin: user.isAdmin,
          hasSharingBan,
        },
        config.jwtSecret,
      );

      res.json({
        token,
        user: {
          id: user.id,
          email: user.email,
          displayName: user.displayName,
          avatarUrl: user.avatarUrl,
          isAdmin: user.isAdmin,
        },
      });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
