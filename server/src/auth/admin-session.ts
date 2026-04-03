import { Router } from 'express';
import session from 'express-session';
import { config } from '../config';
import { verifyGoogleToken } from './google-oauth';
import type { Repositories } from '../repositories/factory';

declare module 'express-session' {
  interface SessionData {
    userId?: string;
    email?: string;
    displayName?: string;
    isAdmin?: boolean;
  }
}

export function createAdminSessionRouter(repos: Repositories): Router {
  const router = Router();

  router.use(
    session({
      secret: config.sessionSecret,
      resave: false,
      saveUninitialized: false,
      cookie: { secure: false, maxAge: 24 * 60 * 60 * 1000 },
    })
  );

  // GET /admin/login — render login page
  router.get('/admin/login', (req, res) => {
    // If EJS is configured, use res.render; otherwise fall back to basic HTML
    if (res.app.get('view engine') === 'ejs') {
      res.render('login', { googleClientId: config.googleClientId });
    } else {
      res.send(`<!DOCTYPE html>
<html>
  <head><title>Admin Login</title></head>
  <body>
    <h1>Admin Login</h1>
    <p>Google Client ID: ${config.googleClientId}</p>
  </body>
</html>`);
    }
  });

  // POST /admin/login — handle Google credential from Sign-In button
  router.post('/admin/login', async (req, res) => {
    const { credential } = req.body as { credential?: string };

    if (!credential) {
      res.status(400).json({ error: 'Missing credential' });
      return;
    }

    const googleUser = await verifyGoogleToken(credential, config.googleClientId);
    if (!googleUser) {
      res.status(401).json({ error: 'Invalid Google token' });
      return;
    }

    const user = await repos.users.getByGoogleId(googleUser.googleId);
    if (!user) {
      res.status(403).json({ error: 'User not found' });
      return;
    }

    if (!user.isAdmin) {
      res.status(403).json({ error: 'Admin access required' });
      return;
    }

    req.session.userId = user.id;
    req.session.email = user.email;
    req.session.displayName = user.displayName;
    req.session.isAdmin = true;

    res.redirect('/admin');
  });

  // GET /admin/logout — destroy session
  router.get('/admin/logout', (req, res) => {
    req.session.destroy(() => {
      res.redirect('/admin/login');
    });
  });

  return router;
}
