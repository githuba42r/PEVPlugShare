import type { GroupRepository, NotificationRepository } from '../repositories/interfaces';
import type { FriendGroup, GroupInvitation } from '../models';
import { AppError } from '../middleware/error-handler';

export class GroupService {
  constructor(
    private groupRepo: GroupRepository,
    private notificationRepo: NotificationRepository,
    private serverBaseUrl: string,
  ) {}

  async createGroup(ownerId: string, name: string, description?: string): Promise<FriendGroup> {
    return this.groupRepo.create(ownerId, name, description);
  }

  async addMembers(groupId: string, userIds: string[], adminUserId: string): Promise<void> {
    const isAdmin = await this.groupRepo.isAdmin(groupId, adminUserId);
    if (!isAdmin) {
      throw new AppError(403, 'Only group admins can add members');
    }

    for (const userId of userIds) {
      await this.groupRepo.addMember(groupId, userId, 'MEMBER');
      await this.notificationRepo.create(userId, 'GROUP_INVITATION', {
        groupId,
        addedBy: adminUserId,
      });
    }
  }

  async generateInvite(
    groupId: string,
    userId: string,
  ): Promise<{ token: string; url: string }> {
    const isMember = await this.groupRepo.isMember(groupId, userId);
    if (!isMember) {
      throw new AppError(403, 'Only group members can generate invitations');
    }

    const invitation = await this.groupRepo.createInvitation(groupId, userId, userId);
    const token = invitation.id;
    const url = `${this.serverBaseUrl}/api/groups/invite/${token}`;

    return { token, url };
  }

  async claimInvite(token: string, userId: string): Promise<GroupInvitation> {
    // token is the invitation ID
    const invitation = await this.groupRepo.getInvitationById(token);
    if (!invitation) {
      throw new AppError(404, 'Invitation not found');
    }

    if (invitation.status !== 'PENDING') {
      throw new AppError(400, 'Invitation is no longer pending');
    }

    return invitation;
  }

  async approveInvitation(
    invitationId: string,
    adminUserId: string,
    groupId: string,
  ): Promise<void> {
    const isAdmin = await this.groupRepo.isAdmin(groupId, adminUserId);
    if (!isAdmin) {
      throw new AppError(403, 'Only group admins can approve invitations');
    }

    const invitation = await this.groupRepo.getInvitationById(invitationId);
    if (!invitation) {
      throw new AppError(404, 'Invitation not found');
    }

    await this.groupRepo.updateInvitationStatus(invitationId, 'ACCEPTED', adminUserId);
    await this.groupRepo.addMember(groupId, invitation.inviteeId, 'MEMBER');

    await this.notificationRepo.create(invitation.inviteeId, 'GROUP_INVITATION_ACCEPTED', {
      groupId,
      approvedBy: adminUserId,
    });
  }

  async declineInvitation(
    invitationId: string,
    adminUserId: string,
    groupId: string,
  ): Promise<void> {
    const isAdmin = await this.groupRepo.isAdmin(groupId, adminUserId);
    if (!isAdmin) {
      throw new AppError(403, 'Only group admins can decline invitations');
    }

    await this.groupRepo.updateInvitationStatus(invitationId, 'DECLINED', adminUserId);
  }
}
