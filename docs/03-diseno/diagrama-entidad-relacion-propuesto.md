# Diagrama entidad-relación propuesto
```mermaid
erDiagram
    PERFIL ||--o{ USUARIO : asigna
    PERFIL ||--o{ PERFIL_ACTIVIDAD : contiene
    ACTIVIDAD ||--o{ PERFIL_ACTIVIDAD : habilita
    USUARIO ||--o{ BITACORA_EVENTO : genera
    USUARIO ||--o{ CARGA_FACTURACION : realiza
    PLANTILLA_CARGA ||--o{ CARGA_FACTURACION : valida
    CARGA_FACTURACION ||--o{ ERROR_CARGA : reporta

    PERFIL {
        bigint perfil_id PK
        nvarchar nombre UK
        nvarchar descripcion
        bit activo
        datetime2 creado_en_utc
        datetime2 actualizado_en_utc
    }

    USUARIO {
        bigint usuario_id PK
        bigint perfil_id FK
        nvarchar clave UK
        nvarchar nombre
        nvarchar correo UK
        nvarchar password_hash
        bit correo_verificado
        bit es_usuario_sat
        bit bloqueado
        bit activo
        datetime2 vigencia_hasta_utc
        datetime2 ultimo_acceso_utc
        datetime2 creado_en_utc
        datetime2 actualizado_en_utc
    }

    ACTIVIDAD {
        bigint actividad_id PK
        nvarchar clave UK
        nvarchar nombre
        nvarchar recurso
        nvarchar accion
        bit activo
        datetime2 creado_en_utc
        datetime2 actualizado_en_utc
    }

    PERFIL_ACTIVIDAD {
        bigint perfil_id PK, FK
        bigint actividad_id PK, FK
        bigint asignado_por_usuario_id FK
        datetime2 asignado_en_utc
    }

    BITACORA_EVENTO {
        bigint evento_id PK
        bigint usuario_id FK
        nvarchar modulo
        nvarchar accion
        nvarchar resultado
        nvarchar detalle_seguro
        nvarchar correlation_id
        datetime2 fecha_accion_utc
        nvarchar canal
        nvarchar ip_origen
    }

    PLANTILLA_CARGA {
        bigint plantilla_id PK
        nvarchar nombre
        nvarchar version
        nvarchar extensiones_permitidas
        nvarchar definicion_columnas_json
        bit activa
        datetime2 creado_en_utc
    }

    CARGA_FACTURACION {
        bigint carga_id PK
        bigint usuario_id FK
        bigint plantilla_id FK
        nvarchar nombre_archivo
        nvarchar hash_sha256
        nvarchar estado
        int total_filas
        int filas_validas
        int filas_invalidas
        nvarchar correlation_id
        datetime2 creado_en_utc
        datetime2 confirmado_en_utc
    }

    ERROR_CARGA {
        bigint error_id PK
        bigint carga_id FK
        nvarchar hoja
        int fila
        nvarchar columna
        nvarchar codigo_regla
        nvarchar valor_enmascarado
        nvarchar mensaje
        datetime2 creado_en_utc
    }
```

## Relaciones y reglas principales

| Relación | Cardinalidad | Regla |
|---|---|---|
| Perfil → Usuario | 1:N | Un usuario pertenece a un perfil operativo. Si se requieren varios perfiles por usuario, se sustituye por una tabla puente tras validación de negocio. |
| Perfil ↔ Actividad | N:M | `perfil_actividad` define permisos explícitos; evita privilegios implícitos. |
| Usuario → BitácoraEvento | 1:N | Todo evento crítico identifica al usuario responsable o una cuenta técnica controlada. |
| Usuario → CargaFacturacion | 1:N | Permite saber quién inició y confirmó cada carga. |
| PlantillaCarga → CargaFacturacion | 1:N | Cada lote conserva la versión de plantilla con que fue validado. |
| CargaFacturacion → ErrorCarga | 1:N | Una carga puede contener cero o más errores localizables. |
