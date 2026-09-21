import { HttpErrorResponse } from '@angular/common/http';

interface ApiErrorBody {
  message?: string;
  correlationId?: string;
}

/** Traduce errores HTTP a mensajes seguros, sin exponer SQL ni detalles internos. */
export function userFacingApiError(error: unknown, fallback: string): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  const body = error.error as ApiErrorBody | null;
  const reference = body?.correlationId ? ` Referencia: ${body.correlationId}` : '';

  if (error.status === 0) return 'No pudimos conectar con el servicio. Revisa tu conexión.';
  if (error.status === 401) return 'Tu sesión terminó. Inicia sesión nuevamente.';
  if (error.status === 403) return 'No tienes permiso para consultar este catálogo.';
  if (error.status === 503) return `El servicio no está disponible.${reference}`;

  return `${body?.message || fallback}${reference}`;
}
