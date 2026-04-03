import type { Request, Response, NextFunction } from 'express';
import { verifyToken } from './jwt';

declare global {
  namespace Express {
    interface Request {
      userId?: string;
      email?: string;
      isAdmin?: boolean;
      hasSharingBan?: boolean;
      userGroupIds?: string[];
    }
  }
}

export function createAuthMiddleware(jwtSecret: string) {
  return function authMiddleware(req: Request, res: Response, next: NextFunction): void {
    const authHeader = req.headers['authorization'];

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const token = authHeader.slice('Bearer '.length);
    const payload = verifyToken(token, jwtSecret);

    if (!payload) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    req.userId = payload.userId;
    req.email = payload.email;
    req.isAdmin = payload.isAdmin;
    req.hasSharingBan = payload.hasSharingBan;
    req.userGroupIds = [];

    next();
  };
}
