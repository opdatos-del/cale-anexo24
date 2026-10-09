import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notifications/notification.service';
import { ConfirmService } from '@core/ui/confirm-dialog/confirm.service';
import { ActaImportErrorCsvService } from '@features/operations/actas/application/acta-import-error-csv.service';
import { UploadActaUseCase } from '@features/operations/actas/application/use-cases/upload-acta.use-case';
import { GetActaUseCase } from '@features/operations/actas/application/use-cases/get-acta.use-case';
import { ConfirmActaUseCase } from '@features/operations/actas/application/use-cases/confirm-acta.use-case';
import { ActaConfirmation, ActaLoad } from '@features/operations/actas/domain/models/acta-import.model';
import { ActaUploadPage } from './acta-upload.page';

interface PageHarness {
  load: { set: (value: ActaLoad) => void };
  confirm: () => void;
  confirmation: () => ActaConfirmation | null;
}

const confirmacionOk: ActaConfirmation = {
  cargaId: 1, estado: 'CONFIRMADA', totalFilas: 2, filasValidas: 2, filasConError: 0,
  confirmadaEn: '2026-10-05T10:15:30', resultado: 'CONFIRMED',
};

describe('ActaUploadPage', () => {
  const cargada: ActaLoad = {
    id: 1, tipo: 'ACTA', archivo: 'actas.xlsx', hash: 'h', estado: 'PREVISUALIZADA',
    totalFilas: 2, filasValidas: 2, filasInvalidas: 0,
    columnas: ['Folio', 'Fecha', 'Clave', 'Linea', 'Cantidad', 'Umc', 'DescargaDirigida', 'ValorComercial'],
    filas: [{ Folio: 'ACC-1' }], totalPersistido: 2, errores: [],
  };
  const confirmada: ActaLoad = { ...cargada, estado: 'CONFIRMADA' };

  function configure(options: { permiso?: boolean; confirmacion?: ActaConfirmation; fallo?: unknown; dialogo?: boolean } = {}) {
    const confirmUseCase = {
      execute: vi.fn(() => (options.fallo ? throwError(() => options.fallo) : of(options.confirmacion ?? confirmacionOk))),
    };
    const getUseCase = { execute: vi.fn(() => of(confirmada)) };
    const dialog = { ask: vi.fn(() => of(options.dialogo ?? true)) };
    const notifications = { success: vi.fn(), info: vi.fn(), error: vi.fn(), warning: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ActaUploadPage],
      providers: [
        { provide: UploadActaUseCase, useValue: { execute: vi.fn(() => of(cargada)) } },
        { provide: ConfirmActaUseCase, useValue: confirmUseCase },
        { provide: GetActaUseCase, useValue: getUseCase },
        { provide: ActaImportErrorCsvService, useValue: { download: vi.fn(() => of(void 0)) } },
        { provide: ConfirmService, useValue: dialog },
        { provide: NotificationService, useValue: notifications },
        { provide: AuthService, useValue: { hasPermission: vi.fn(() => options.permiso ?? true) } },
      ],
    });
    const fixture = TestBed.createComponent(ActaUploadPage);
    fixture.detectChanges();
    const harness = fixture.componentInstance as unknown as PageHarness;
    return { fixture, harness, confirmUseCase, getUseCase, dialog, notifications };
  }

  function texto(fixture: { nativeElement: HTMLElement }): string {
    return fixture.nativeElement.textContent ?? '';
  }

  it('sin permiso no muestra el botón Confirmar', () => {
    const { fixture, harness } = configure({ permiso: false });
    harness.load.set(cargada);
    fixture.detectChanges();
    expect(texto(fixture)).not.toContain('Confirmar');
  });

  it('con permiso y carga previsualizada muestra el botón y las columnas ACTA', () => {
    const { fixture, harness } = configure();
    harness.load.set(cargada);
    fixture.detectChanges();
    expect(texto(fixture)).toContain('Confirmar');
    expect(texto(fixture)).toContain('Lista para confirmar');
    expect(texto(fixture)).toContain('DescargaDirigida');
  });

  it('con errores no es confirmable', () => {
    const { fixture, harness } = configure();
    harness.load.set({ ...cargada, estado: 'CON_ERRORES', errores: [{ hoja: 'A', fila: 2, columna: 'Linea', valorEnmascarado: null, codigo: 'LINEA_ACTA_INVALIDA', mensaje: 'x' }] });
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

  it('el aviso usa reglas vigentes y no la palabra irreversible', () => {
    const { harness, dialog } = configure();
    harness.load.set(cargada);
    harness.confirm();
    const message = (dialog.ask.mock.calls[0] as unknown as [{ message: string }])[0].message;
    expect(message).toContain('reglas vigentes del sistema Anexo 24');
    expect(message).toContain('salidas, partidas y descargas dirigidas');
    expect(message).not.toContain('irreversible');
  });

  it('confirmar dispara exactamente una llamada y refresca', () => {
    const { fixture, harness, confirmUseCase, getUseCase, notifications } = configure();
    harness.load.set(cargada);
    harness.confirm();
    fixture.detectChanges();
    expect(confirmUseCase.execute).toHaveBeenCalledWith(1);
    expect(getUseCase.execute).toHaveBeenCalledWith(1, 1, 100);
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

  it('409 etapa ocupada muestra mensaje seguro', () => {
    const { harness, notifications } = configure({ fallo: new HttpErrorResponse({ status: 409, error: { message: 'La operación entra en conflicto con el estado actual del recurso.' } }) });
    harness.load.set(cargada);
    harness.confirm();
    expect(notifications.error).toHaveBeenCalledWith(expect.stringContaining('conflicto'));
  });

  it('422 no procesable muestra mensaje seguro sin detalles SQL', () => {
    const { harness, notifications } = configure({ fallo: new HttpErrorResponse({ status: 422, error: { message: 'La carga no puede confirmarse en su estado o contrato actual.' } }) });
    harness.load.set(cargada);
    harness.confirm();
    const mensaje = notifications.error.mock.calls[0][0] as string;
    expect(mensaje).toContain('La carga no puede confirmarse');
    expect(mensaje).not.toMatch(/SQL|procedure|SELECT|INSERT|516/i);
  });
  it('cambia de página sin volver a subir el archivo', () => {
    const { fixture, harness, getUseCase } = configure();
    harness.load.set({ ...cargada, totalPersistido: 101 });
    (harness as unknown as { changePreviewPage: (event: { pageIndex: number; pageSize: number }) => void }).changePreviewPage({ pageIndex: 1, pageSize: 25 });
    fixture.detectChanges();
    expect(getUseCase.execute).toHaveBeenCalledWith(1, 2, 25);
  });

});
