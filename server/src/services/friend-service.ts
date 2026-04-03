import type { FriendRepository, UserRepository, NotificationRepository } from '../repositories/interfaces';
import type { Friendship } from '../models';
import { AppError } from '../middleware/error-handler';

export class FriendService {
  constructor(
    private friendRepo: FriendRepository,
    private userRepo: UserRepository,
    private notificationRepo: NotificationRepository,
  ) {}

  async sendRequest(
    requesterId: string,
    addresseeEmail?: string,
    addresseeId?: string,
  ): Promise<Friendship> {
    // Resolve addressee
    if (!addresseeEmail && !addresseeId) {
      throw new AppError(400, 'Must provide email or userId');
    }

    let resolvedId = addresseeId;
    if (!resolvedId && addresseeEmail) {
      const user = await this.userRepo.getByEmail(addresseeEmail);
      if (!user) throw new AppError(404, 'User not found');
      resolvedId = user.id;
    }

    if (requesterId === resolvedId) {
      throw new AppError(400, 'Cannot send friend request to yourself');
    }

    const already = await this.friendRepo.areFriends(requesterId, resolvedId!);
    if (already) {
      throw new AppError(409, 'Already friends with this user');
    }

    const friendship = await this.friendRepo.sendRequest(requesterId, resolvedId!);

    // Notify addressee
    await this.notificationRepo.create(resolvedId!, 'FRIEND_REQUEST', {
      friendshipId: friendship.id,
      requesterId,
    });

    return friendship;
  }

  async acceptRequest(requestId: string, userId: string): Promise<Friendship> {
    const request = await this.friendRepo.getRequestById(requestId);
    if (!request) throw new AppError(404, 'Friend request not found');
    if (request.addresseeId !== userId) {
      throw new AppError(403, 'Not authorized to accept this request');
    }

    const updated = await this.friendRepo.acceptRequest(requestId);

    // Notify requester
    await this.notificationRepo.create(request.requesterId, 'FRIEND_ACCEPTED', {
      friendshipId: updated.id,
      acceptedBy: userId,
    });

    return updated;
  }

  async declineRequest(requestId: string, userId: string): Promise<Friendship> {
    const request = await this.friendRepo.getRequestById(requestId);
    if (!request) throw new AppError(404, 'Friend request not found');
    if (request.addresseeId !== userId) {
      throw new AppError(403, 'Not authorized to decline this request');
    }

    return this.friendRepo.declineRequest(requestId);
  }

  async removeFriend(userId: string, friendUserId: string): Promise<boolean> {
    return this.friendRepo.removeFriend(userId, friendUserId);
  }
}
