import {
  ChangeUserExpirationCommand,
  ChangeUserStatusCommand,
  ResetUserPasswordCommand,
  UpdateUserCommand,
  UserAdministration,
  UserPage,
} from '../../../domain/models/user-administration.model';
import {
  ChangeUserExpirationRequestDto,
  ChangeUserStatusRequestDto,
  ResetUserPasswordRequestDto,
  UpdateUserRequestDto,
  UserPageResponseDto,
  UserResponseDto,
} from '../dto/user.dto';

/** Traduce entre el contrato HTTP en español y el dominio frontend. */
export const UserMapper = {
  toDomain(dto: UserResponseDto): UserAdministration {
    return {
      id: dto.id,
      key: dto.clave,
      name: dto.nombre,
      email: dto.correo,
      status: dto.estado,
      expiration: dto.vigencia,
      profileId: dto.perfilId,
      profileName: dto.perfilNombre,
    };
  },

  toPage(dto: UserPageResponseDto): UserPage {
    return {
      items: dto.items.map((item) => UserMapper.toDomain(item)),
      total: dto.total,
      page: dto.pagina,
      pageSize: dto.tamano,
    };
  },

  toUpdateRequest(command: UpdateUserCommand): UpdateUserRequestDto {
    return { nombre: command.name, correo: command.email };
  },

  toStatusRequest(command: ChangeUserStatusCommand): ChangeUserStatusRequestDto {
    return { estado: command.status };
  },

  toExpirationRequest(command: ChangeUserExpirationCommand): ChangeUserExpirationRequestDto {
    return { vigencia: command.expiration };
  },

  toPasswordRequest(command: ResetUserPasswordCommand): ResetUserPasswordRequestDto {
    return { password: command.password };
  },
};
