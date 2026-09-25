import { ProfileActivity, ProfilePermissions } from '@features/administration/profiles/domain/models/profile-permission.model';
import { ProfileAdministration } from '@features/administration/profiles/domain/models/profile-administration.model';
import {
  ProfileActivityResponseDto,
  ProfilePermissionsResponseDto,
} from '@features/administration/profiles/infrastructure/api/dto/profile-management.dto';
import { ProfileResponseDto } from '@features/administration/profiles/infrastructure/api/dto/profile.dto';
import { ProfileMapper } from './profile.mapper';

/** Traduce respuestas de administración al modelo frontend. */
export const ProfileManagementMapper = {
  toActivity(dto: ProfileActivityResponseDto): ProfileActivity {
    return {
      id: dto.id,
      key: dto.clave,
      name: dto.nombre,
      resource: dto.recurso,
      action: dto.accion,
    };
  },

  toPermissions(dto: ProfilePermissionsResponseDto): ProfilePermissions {
    return {
      profileId: dto.perfilId,
      permissions: dto.permisos.map(ProfileManagementMapper.toActivity),
    };
  },

  toProfile(dto: ProfileResponseDto): ProfileAdministration {
    return ProfileMapper.toDomain(dto);
  },
};
