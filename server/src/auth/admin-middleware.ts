import type { Request, Response, NextFunction } from 'express';

/**
 * requireAdmin — for /admin/api/* JSON routes.
 * Accepts either a browser session (req.session.isAdmin) or a JWT claim (req.isAdmin).
 */
export function requireAdmin(req: Request, res: Response, next: NextFunction): void {
  const sessionAdmin = (req.session as unknown as Record<string, unknown> | undefined)?.['isAdmin'] === true;
  const jwtAdmin = req.isAdmin === true;

  if (sessionAdmin || jwtAdmin) {
    next();
    return;
  }

  res.status(403).json({ error: 'Admin access required' });
}

/**
 * requireAdminSession — for /admin/* view routes.
 * Requires an active browser session with userId and isAdmin set.
 * Redirects to /admin/login if not authenticated.
 */
export function requireAdminSession(req: Request, res: Response, next: NextFunction): void {
  const session = req.session as unknown as Record<string, unknown> | undefined;
  const hasSession = session?.['userId'] && session?.['isAdmin'] === true;

  if (hasSession) {
    next();
    return;
  }

  res.redirect('/admin/login');
}
