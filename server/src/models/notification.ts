export type NotificationType =
  | 'FRIEND_REQUEST'
  | 'FRIEND_ACCEPTED'
  | 'GROUP_INVITATION'
  | 'GROUP_INVITATION_ACCEPTED'
  | 'GROUP_INVITATION_DECLINED'
  | 'LOCATION_SHARED'
  | 'SUGGESTION_ACCEPTED'
  | 'SUGGESTION_REJECTED'
  | 'LOCATION_UPDATED'
  | 'SYSTEM_MESSAGE';

export interface Notification {
  id: string;
  userId: string;
  type: NotificationType;
  title: string;
  body: string;
  data: Record<string, unknown> | null;
  isRead: boolean;
  createdAt: Date;
  readAt: Date | null;
}
