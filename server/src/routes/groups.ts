import { Router } from 'express';
import type { GroupRepository } from '../repositories/interfaces';
import type { GroupService } from '../services/group-service';
import { AppError } from '../middleware/error-handler';

export function createGroupRouter(
  groupService: GroupService,
  groupRepo: GroupRepository,
): Router {
  const router = Router();

  // POST /api/groups — create group
  router.post('/api/groups', async (req, res, next) => {
    try {
      const { name, description } = req.body as { name: string; description?: string };
      if (!name) {
        throw new AppError(400, 'Group name is required');
      }
      const group = await groupService.createGroup(req.userId!, name, description);
      res.status(201).json(group);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/groups — list user's groups
  router.get('/api/groups', async (req, res, next) => {
    try {
      const cursor = req.query['cursor'] as string | undefined;
      const limitRaw = req.query['limit'];
      const limit = limitRaw ? parseInt(limitRaw as string, 10) : undefined;
      const result = await groupRepo.getByUser(req.userId!, cursor, limit);
      res.json(result);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/groups/invite/:token — resolve invite token (static before :id)
  router.get('/api/groups/invite/:token', async (req, res, next) => {
    try {
      const invitation = await groupService.claimInvite(req.params['token']!, req.userId!);
      res.json(invitation);
    } catch (err) {
      next(err);
    }
  });

  // POST /api/groups/invite/:token/accept — claim invite (static before :id)
  router.post('/api/groups/invite/:token/accept', async (req, res, next) => {
    try {
      const invitation = await groupService.claimInvite(req.params['token']!, req.userId!);
      res.json(invitation);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/groups/:id
  router.get('/api/groups/:id', async (req, res, next) => {
    try {
      const group = await groupRepo.getById(req.params['id']!);
      if (!group) throw new AppError(404, 'Group not found');
      res.json(group);
    } catch (err) {
      next(err);
    }
  });

  // PUT /api/groups/:id — update (admin or owner)
  router.put('/api/groups/:id', async (req, res, next) => {
    try {
      const groupId = req.params['id']!;
      const isAdmin = await groupRepo.isAdmin(groupId, req.userId!);
      if (!isAdmin) throw new AppError(403, 'Only group admins can update the group');
      const { name, description } = req.body as { name?: string; description?: string };
      const updated = await groupRepo.update(groupId, { name, description });
      res.json(updated);
    } catch (err) {
      next(err);
    }
  });

  // DELETE /api/groups/:id — delete (owner/admin)
  router.delete('/api/groups/:id', async (req, res, next) => {
    try {
      const groupId = req.params['id']!;
      const isAdmin = await groupRepo.isAdmin(groupId, req.userId!);
      if (!isAdmin) throw new AppError(403, 'Only group admins can delete the group');
      const deleted = await groupRepo.delete(groupId);
      res.json({ deleted });
    } catch (err) {
      next(err);
    }
  });

  // GET /api/groups/:id/members — must be member
  router.get('/api/groups/:id/members', async (req, res, next) => {
    try {
      const groupId = req.params['id']!;
      const isMember = await groupRepo.isMember(groupId, req.userId!);
      if (!isMember) throw new AppError(403, 'Only group members can view members');
      const members = await groupRepo.getMembers(groupId);
      res.json(members);
    } catch (err) {
      next(err);
    }
  });

  // POST /api/groups/:id/members — add members (admin/owner)
  router.post('/api/groups/:id/members', async (req, res, next) => {
    try {
      const groupId = req.params['id']!;
      const { userIds } = req.body as { userIds: string[] };
      if (!Array.isArray(userIds) || userIds.length === 0) {
        throw new AppError(400, 'userIds must be a non-empty array');
      }
      await groupService.addMembers(groupId, userIds, req.userId!);
      res.status(201).json({ added: userIds.length });
    } catch (err) {
      next(err);
    }
  });

  // DELETE /api/groups/:id/members/:userId — remove member (admin/owner)
  router.delete('/api/groups/:id/members/:userId', async (req, res, next) => {
    try {
      const groupId = req.params['id']!;
      const targetUserId = req.params['userId']!;
      const isAdmin = await groupRepo.isAdmin(groupId, req.userId!);
      if (!isAdmin) throw new AppError(403, 'Only group admins can remove members');
      const removed = await groupRepo.removeMember(groupId, targetUserId);
      res.json({ removed });
    } catch (err) {
      next(err);
    }
  });

  // PUT /api/groups/:id/members/:userId/role — change role (owner/admin)
  router.put('/api/groups/:id/members/:userId/role', async (req, res, next) => {
    try {
      const groupId = req.params['id']!;
      const targetUserId = req.params['userId']!;
      const { role } = req.body as { role: 'ADMIN' | 'MEMBER' };
      if (role !== 'ADMIN' && role !== 'MEMBER') {
        throw new AppError(400, 'Role must be ADMIN or MEMBER');
      }
      const isAdmin = await groupRepo.isAdmin(groupId, req.userId!);
      if (!isAdmin) throw new AppError(403, 'Only group admins can change member roles');
      const member = await groupRepo.setMemberRole(groupId, targetUserId, role);
      res.json(member);
    } catch (err) {
      next(err);
    }
  });

  // POST /api/groups/:id/invite — generate invite (member)
  router.post('/api/groups/:id/invite', async (req, res, next) => {
    try {
      const groupId = req.params['id']!;
      const result = await groupService.generateInvite(groupId, req.userId!);
      res.status(201).json(result);
    } catch (err) {
      next(err);
    }
  });

  // GET /api/groups/:id/invitations — list pending invitations (admin/owner)
  router.get('/api/groups/:id/invitations', async (req, res, next) => {
    try {
      const groupId = req.params['id']!;
      const isAdmin = await groupRepo.isAdmin(groupId, req.userId!);
      if (!isAdmin) throw new AppError(403, 'Only group admins can view invitations');
      const invitations = await groupRepo.getPendingInvitations(groupId);
      res.json(invitations);
    } catch (err) {
      next(err);
    }
  });

  // PUT /api/groups/:id/invitations/:inviteId/approve — approve (admin/owner)
  router.put('/api/groups/:id/invitations/:inviteId/approve', async (req, res, next) => {
    try {
      const groupId = req.params['id']!;
      const inviteId = req.params['inviteId']!;
      await groupService.approveInvitation(inviteId, req.userId!, groupId);
      res.json({ approved: true });
    } catch (err) {
      next(err);
    }
  });

  // PUT /api/groups/:id/invitations/:inviteId/decline — decline (admin/owner)
  router.put('/api/groups/:id/invitations/:inviteId/decline', async (req, res, next) => {
    try {
      const groupId = req.params['id']!;
      const inviteId = req.params['inviteId']!;
      await groupService.declineInvitation(inviteId, req.userId!, groupId);
      res.json({ declined: true });
    } catch (err) {
      next(err);
    }
  });

  return router;
}
