import {
  ChangeUserExpirationCommand,
  ChangeUserProfileCommand,
  ChangeUserStatusCommand,
  CreateUserCommand,
  ResetUserPasswordCommand,
  UpdateUserCommand,
  UserAdministration,
  UserPage,
} from '@features/administration/users/domain/models/user-administration.model';
import {
  ChangeUserExpirationRequestDto,
  ChangeUserProfileRequestDto,
  ChangeUserStatusRequestDto,
  CreateUserRequestDto,
  ResetUserPasswordRequestDto,
  UpdateUserRequestDto,
  UserPageResponseDto,
  UserResponseDto,
} from '@features/administration/users/infrastructure/api/dto/user.dto';

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

  toCreateRequest(command: CreateUserCommand): CreateUserRequestDto {
    return {
      clave: command.key,
      nombre: command.name,
      correo: command.email,
      password: command.password,
      vigencia: command.expiration,
      perfilId: command.profileId,
    };
  },

  toProfileRequest(command: ChangeUserProfileCommand): ChangeUserProfileRequestDto {
    return { perfilId: command.profileId };
  },
};
