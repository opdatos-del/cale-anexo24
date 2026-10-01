import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notifications/notification.service';
import { ConfirmService } from '@core/ui/confirm-dialog/confirm.service';
import { UploadPedimentUseCase } from '@features/operations/pediments/application/use-cases/upload-pediment.use-case';
import { ConfirmPedimentUseCase } from '@features/operations/pediments/application/use-cases/confirm-pediment.use-case';
import { GetPedimentUseCase } from '@features/operations/pediments/application/use-cases/get-pediment.use-case';
import { PedimentConfirmation, PedimentLoad } from '@features/operations/pediments/domain/models/pediment-upload.model';
import { PedimentUploadPage } from './pediment-upload.page';

interface PageHarness {
  load: { set: (value: PedimentLoad) => void };
  confirm: () => void;
  confirmation: () => PedimentConfirmation | null;
}

describe('PedimentUploadPage', () => {
  const cargada: PedimentLoad = {
    id: 1,
    archivo: 'pedimentos.xlsx',
    hash: 'h',
    estado: 'PREVISUALIZADA',
    totalFilas: 2,
    filasValidas: 2,
    filasInvalidas: 0,
    versionPlantilla: 'LEGACY-STAGE-DERIVED-V2',
    correlationId: 'c',
    preview: { columnas: [], filas: [], pagina: 1, tamano: 100, totalFilas: 2 },
    errores: [],
  };

  const confirmada: PedimentLoad = { ...cargada, estado: 'CONFIRMADA' };

  function configure(options: { permiso?: boolean; confirmacion?: PedimentConfirmation; fallo?: unknown; dialogo?: boolean } = {}) {
    const confirmUseCase = {
      execute: vi.fn(() => (options.fallo ? throwError(() => options.fallo) : of(options.confirmacion ?? confirmacionOk))),
    };
    const getUseCase = { execute: vi.fn(() => of(confirmada)) };
    const dialog = { ask: vi.fn(() => of(options.dialogo ?? true)) };
    const notifications = { success: vi.fn(), info: vi.fn(), error: vi.fn(), warning: vi.fn() };
    TestBed.configureTestingModule({
      imports: [PedimentUploadPage],
      providers: [
        { provide: UploadPedimentUseCase, useValue: { execute: vi.fn(() => of()) } },
        { provide: ConfirmPedimentUseCase, useValue: confirmUseCase },
        { provide: GetPedimentUseCase, useValue: getUseCase },
        { provide: ConfirmService, useValue: dialog },
        { provide: NotificationService, useValue: notifications },
        { provide: AuthService, useValue: { hasPermission: vi.fn(() => options.permiso ?? true) } },
      ],
    });
    const fixture = TestBed.createComponent(PedimentUploadPage);
    fixture.detectChanges();
    const harness = fixture.componentInstance as unknown as PageHarness;
    return { fixture, harness, confirmUseCase, getUseCase, dialog, notifications };
  }

  const confirmacionOk: PedimentConfirmation = {
    cargaId: 1,
    estado: 'CONFIRMADA',
    resultado: 'CONFIRMED',
    tipoOperacion: 1,
    operacionesProcesadas: 1,
    partidasProcesadas: 2,
    fechaConfirmacion: '2026-05-28T10:15:30',
  };

  function texto(fixture: { nativeElement: HTMLElement }): string {
    return fixture.nativeElement.textContent ?? '';
  }

  it('sin permiso no muestra el botón Confirmar', () => {
    const { fixture, harness } = configure({ permiso: false });
    harness.load.set(cargada);
    fixture.detectChanges();
    expect(texto(fixture)).not.toContain('Confirmar');
  });

  it('con permiso y carga previsualizada muestra el botón', () => {
    const { fixture, harness } = configure();
    harness.load.set(cargada);
    fixture.detectChanges();
    expect(texto(fixture)).toContain('Confirmar');
    expect(texto(fixture)).toContain('Lista para confirmar');
  });

  it('con errores no es confirmable', () => {
    const { fixture, harness } = configure();
    harness.load.set({ ...cargada, estado: 'CON_ERRORES', errores: [{ hoja: 'H', fila: 2, columna: 'Clave', valorEnmascarado: null, codigo: 'PED-003', mensaje: 'x' }] });
    fixture.detectChanges();
    expect(texto(fixture)).not.toContain('Confirmar');
  });

  it('confirmada no muestra botón y sí estado terminal', () => {
    const { fixture, harness } = configure();
    harness.load.set(confirmada);
    fixture.detectChanges();
    expect(texto(fixture)).not.toContain('Confirmar');
    expect(texto(fixture)).toContain('Confirmada');
  });

  it('cancelar el diálogo no dispara ninguna confirmación', () => {
    const { fixture, harness, confirmUseCase, dialog } = configure({ dialogo: false });
    harness.load.set(cargada);
    harness.confirm();
    expect(dialog.ask).toHaveBeenCalledTimes(1);
    expect(confirmUseCase.execute).not.toHaveBeenCalled();
    fixture.detectChanges();
  });

  it('confirmar dispara exactamente una llamada y refresca', () => {
    const { fixture, harness, confirmUseCase, getUseCase, notifications } = configure();
    harness.load.set(cargada);
    harness.confirm();
    fixture.detectChanges();
    expect(confirmUseCase.execute).toHaveBeenCalledWith(1);
    expect(getUseCase.execute).toHaveBeenCalledWith(1);
    expect(notifications.success).toHaveBeenCalled();
    expect(harness.confirmation()?.resultado).toBe('CONFIRMED');
  });

  it('doble click no duplica la llamada', () => {
    const { harness, confirmUseCase } = configure();
    harness.load.set(cargada);
    harness.confirm();
    harness.confirm();
    expect(confirmUseCase.execute).toHaveBeenCalledTimes(1);
  });

  it('ALREADY_CONFIRMED notifica información y refresca', () => {
    const { harness, notifications, getUseCase } = configure({ confirmacion: { ...confirmacionOk, resultado: 'ALREADY_CONFIRMED', operacionesProcesadas: 0, partidasProcesadas: 0 } });
    harness.load.set(cargada);
    harness.confirm();
    expect(notifications.info).toHaveBeenCalledWith('La carga ya se encontraba confirmada.');
    expect(getUseCase.execute).toHaveBeenCalledWith(1);
  });

  it('409 muestra mensaje seguro', () => {
    const { harness, notifications } = configure({ fallo: new HttpErrorResponse({ status: 409, error: { message: 'El pedimento ya existe en la operación autoritativa.' } }) });
    harness.load.set(cargada);
    harness.confirm();
    expect(notifications.error).toHaveBeenCalledWith(expect.stringContaining('El pedimento ya existe'));
  });

  it('422 muestra mensaje seguro sin detalles SQL', () => {
    const { harness, notifications } = configure({ fallo: new HttpErrorResponse({ status: 422, error: { message: 'La carga no puede confirmarse en su estado o contrato actual.' } }) });
    harness.load.set(cargada);
    harness.confirm();
    const mensaje = notifications.error.mock.calls[0][0] as string;
    expect(mensaje).toContain('La carga no puede confirmarse');
    expect(mensaje).not.toMatch(/SQL|procedure|SELECT|INSERT|514/i);
  });
});
