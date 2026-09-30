import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { of } from 'rxjs';
import { NotificationService } from '@core/notifications/notification.service';
import { UploadPedimentUseCase } from '@features/operations/pediments/application/use-cases/upload-pediment.use-case';
import { PedimentUploadPage } from './pediment-upload.page';

describe('PedimentUploadPage', () => {
  function configure() {
    const useCase = { execute: vi.fn(() => of()) };
    TestBed.configureTestingModule({
      imports: [PedimentUploadPage],
      providers: [
        { provide: UploadPedimentUseCase, useValue: useCase },
        { provide: NotificationService, useValue: { error: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(PedimentUploadPage);
    fixture.detectChanges();
    return { fixture, useCase };
  }

  it('muestra que el contrato es derivado y no ofrece confirmación', () => {
    const { fixture } = configure();
    expect(fixture.nativeElement.textContent).toContain('contrato derivado de CargaPedimentosIE');
    expect(fixture.nativeElement.textContent).toContain('Sin confirmación operativa');
    expect(fixture.nativeElement.textContent).not.toContain('Confirmar');
  });

  it('rechaza extensiones y archivos mayores al límite', () => {
    const { fixture } = configure();
    const page = fixture.componentInstance as unknown as { onFileSelected: (event: Event) => void; selectedFile: () => File | null; selectionMessage: () => string | null };
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [new File(['x'], 'datos.csv')] });
    page.onFileSelected({ target: input } as unknown as Event);
    expect(page.selectedFile()).toBeNull();
    expect(page.selectionMessage()).toContain('Sólo se permiten');
  });
});
