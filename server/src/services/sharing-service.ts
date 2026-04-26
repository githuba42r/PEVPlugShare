import { v4 as uuidv4 } from 'uuid';
import type {
  ShareRepository,
  BanRepository,
  DeepLinkRepository,
  LocationRepository,
  NotificationRepository,
  GroupRepository,
} from '../repositories/interfaces';
import type { LocationShare, HiddenShare, Location } from '../models';
import { AppError } from '../middleware/error-handler';

export class SharingService {
  constructor(
    private shareRepo: ShareRepository,
    private banRepo: BanRepository,
    private deepLinkRepo: DeepLinkRepository,
    private locationRepo: LocationRepository,
    private notificationRepo: NotificationRepository,
    private groupRepo: GroupRepository,
    private serverBaseUrl: string,
  ) {}

  private async checkBan(userId: string): Promise<void> {
    const banned = await this.banRepo.hasActiveSharingBan(userId);
    if (banned) {
      throw new AppError(403, 'You have an active sharing ban');
    }
  }

  private async verifyOwner(locationId: string, userId: string): Promise<Location> {
    const location = await this.locationRepo.getById(locationId);
    if (!location || location.isDeleted) {
      throw new AppError(404, 'Location not found');
    }
    if (location.ownerId !== userId) {
      throw new AppError(403, 'You do not own this location');
    }
    return location;
  }

  async shareWithUser(
    locationId: string,
    userId: string,
    sharedBy: string,
  ): Promise<LocationShare> {
    await this.checkBan(sharedBy);
    await this.verifyOwner(locationId, sharedBy);

    const share = await this.shareRepo.shareWithUser(locationId, sharedBy, userId);

    await this.notificationRepo.create(userId, 'LOCATION_SHARED', {
      locationId,
      shareId: share.id,
      sharedBy,
    });

    return share;
  }

  async shareWithGroup(
    locationId: string,
    groupId: string,
    sharedBy: string,
  ): Promise<LocationShare> {
    await this.checkBan(sharedBy);
    await this.verifyOwner(locationId, sharedBy);

    const share = await this.shareRepo.shareWithGroup(locationId, sharedBy, groupId);

    const members = await this.groupRepo.getMembers(groupId);
    for (const member of members) {
      if (member.userId !== sharedBy) {
        await this.notificationRepo.create(member.userId, 'LOCATION_SHARED', {
          locationId,
          shareId: share.id,
          sharedBy,
          groupId,
        });
      }
    }

    return share;
  }

  async revokeShare(shareId: string, locationId: string, userId: string): Promise<boolean> {
    // Verify the requesting user owns the share (is the sharer) by checking location ownership
    await this.verifyOwner(locationId, userId);
    return this.shareRepo.revokeShare(shareId);
  }

  async generateDirectLink(
    locationId: string,
    userId: string,
    options?: { singleUse?: boolean; maxUses?: number; expiresAt?: Date },
  ): Promise<{ token: string; url: string }> {
    await this.verifyOwner(locationId, userId);

    const token = uuidv4();
    await this.deepLinkRepo.createToken(
      locationId,
      userId,
      token,
      options?.singleUse,
      options?.maxUses,
      options?.expiresAt,
    );

    const url = `${this.serverBaseUrl}/share/${token}`;
    return { token, url };
  }

  async resolveDirectLink(token: string): Promise<Location> {
    const tokenRecord = await this.deepLinkRepo.getByToken(token);
    if (!tokenRecord) {
      throw new AppError(404, 'Share link not found or expired');
    }

    // Check expiry
    if (tokenRecord.expiresAt && tokenRecord.expiresAt < new Date()) {
      throw new AppError(404, 'Share link has expired');
    }

    // Check max uses
    if (tokenRecord.maxUses !== null && tokenRecord.useCount >= tokenRecord.maxUses) {
      throw new AppError(404, 'Share link has reached its maximum uses');
    }

    await this.deepLinkRepo.incrementUseCount(tokenRecord.id);

    const location = await this.locationRepo.getById(tokenRecord.locationId);
    if (!location || location.isDeleted) {
      throw new AppError(404, 'Location not found');
    }

    return location;
  }

  async onLocationSoftDelete(locationId: string, reason?: string): Promise<void> {
    // Get shares before deactivation to know who to notify
    const sharesResult = await this.shareRepo.getSharesForLocation(locationId, undefined, 1000);
    const shares = sharesResult.data;

    await this.shareRepo.deactivateSharesForLocation(locationId);

    for (const share of shares) {
      if (share.sharedWithUserId) {
        await this.notificationRepo.create(share.sharedWithUserId, 'SYSTEM_MESSAGE', {
          locationId,
          shareId: share.id,
          reason: reason ?? null,
        });
      }
    }
  }

  async hideShare(userId: string, locationId: string, shareId: string): Promise<HiddenShare> {
    return this.shareRepo.hideShare(userId, locationId, shareId);
  }

  async unhideShare(userId: string, shareId: string): Promise<boolean> {
    return this.shareRepo.unhideShare(userId, shareId);
  }
}
