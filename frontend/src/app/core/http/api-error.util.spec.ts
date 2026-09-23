import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { userFacingApiError } from './api-error.util';

describe('userFacingApiError', () => {
  it('muestra el detalle seguro de un recurso no encontrado con su referencia', () => {
    const error = new HttpErrorResponse({ status: 404, error: { message: 'El usuario ya no existe.', correlationId: 'ref-404' } });
    expect(userFacingApiError(error, 'Alternativa')).toBe('El usuario ya no existe. Referencia: ref-404');
  });

  it('usa el mensaje administrativo por defecto para conflictos', () => {
    const error = new HttpErrorResponse({ status: 409, error: {} });
    expect(userFacingApiError(error, 'Alternativa')).toBe('La operación contradice una regla administrativa.');
  });
});
