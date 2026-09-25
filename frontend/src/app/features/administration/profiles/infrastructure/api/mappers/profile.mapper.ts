import { ProfileAdministration, ProfilePage } from '@features/administration/profiles/domain/models/profile-administration.model';
import { ProfilePageResponseDto, ProfileResponseDto } from '../dto/profile.dto';

/** Traduce entre el contrato HTTP y el dominio de perfiles. */
export const ProfileMapper = {
  toDomain(dto: ProfileResponseDto): ProfileAdministration {
    return {
      id: dto.id,
      name: dto.nombre,
      status: dto.estado,
      permissionCount: dto.cantidadPermisos,
    };
  },

  toPage(dto: ProfilePageResponseDto): ProfilePage {
    return {
      items: dto.items.map((item) => ProfileMapper.toDomain(item)),
      total: dto.total,
      page: dto.pagina,
      pageSize: dto.tamano,
    };
  },
};
