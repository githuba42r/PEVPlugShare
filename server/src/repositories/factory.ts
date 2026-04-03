import type { Knex } from 'knex';
import { config } from '../config';
import type {
  UserRepository,
  RateLimitRepository,
  BanRepository,
  NotificationRepository,
  LocationRepository,
  PhotoRepository,
  FriendRepository,
  GroupRepository,
} from './interfaces';

export interface Repositories {
  users: UserRepository;
  rateLimits: RateLimitRepository;
  bans: BanRepository;
  notifications: NotificationRepository;
  locations: LocationRepository;
  photos: PhotoRepository;
  friends: FriendRepository;
  groups: GroupRepository;
}

export function createRepositories(knex: Knex): Repositories {
  const backend = config.dbBackend;
  const dir = backend === 'mysql' ? './mysql' : './postgres';

  // Use dynamic require to avoid loading both backends
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { PostgresUserRepo } = require(`${dir}/user-repo`);
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { PostgresRateLimitRepo } = require(`${dir}/rate-limit-repo`);
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { PostgresBanRepo } = require(`${dir}/ban-repo`);
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { PostgresNotificationRepo } = require(`${dir}/notification-repo`);
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { PostgresLocationRepo } = require(`${dir}/location-repo`);
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { PostgresPhotoRepo } = require(`${dir}/photo-repo`);
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { PostgresFriendRepo } = require(`${dir}/friend-repo`);
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { PostgresGroupRepo } = require(`${dir}/group-repo`);

  return {
    users: new PostgresUserRepo(knex),
    rateLimits: new PostgresRateLimitRepo(knex),
    bans: new PostgresBanRepo(knex),
    notifications: new PostgresNotificationRepo(knex),
    locations: new PostgresLocationRepo(knex),
    photos: new PostgresPhotoRepo(knex),
    friends: new PostgresFriendRepo(knex),
    groups: new PostgresGroupRepo(knex),
  };
}
