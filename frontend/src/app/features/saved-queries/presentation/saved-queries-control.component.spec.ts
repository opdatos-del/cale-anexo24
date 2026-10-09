import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationService } from '@core/notifications/notification.service';
import { SavedQuery } from '@features/saved-queries/domain/saved-query.model';
import { SavedQueriesApi } from '@features/saved-queries/infrastructure/saved-queries.api';
import { SavedQueriesControlComponent } from './saved-queries-control.component';

interface ControlInternals {
  toggleList(): void;
  openEditor(query?: SavedQuery): void;
  save(): void;
  apply(query: SavedQuery): void;
  remove(query: SavedQuery): void;
  name: string;
  description: string;
}

const query: SavedQuery = {
  id: 7,
  nombre: 'Entradas octubre',
  descripcion: null,
  alcance: 'ENTRADAS',
  criterios: { from: '2026-10-01', to: '2026-10-31', customsDocument: '' },
  fechaCreacion: '2026-10-01T00:00:00',
  fechaActualizacion: '2026-10-02T00:00:00',
};

describe('SavedQueriesControlComponent', () => {
  let fixture: ComponentFixture<SavedQueriesControlComponent>;
  let component: SavedQueriesControlComponent;
  let internals: ControlInternals;
  const api = { list: vi.fn(), create: vi.fn(), update: vi.fn(), delete: vi.fn() };
  const notifications = { success: vi.fn(), error: vi.fn() };

  beforeEach(() => {
    vi.stubGlobal('confirm', vi.fn(() => true));
    api.list.mockReset().mockReturnValue(of([query]));
    api.create.mockReset().mockReturnValue(of(query));
    api.update.mockReset().mockReturnValue(of(query));
    api.delete.mockReset().mockReturnValue(of(void 0));
    notifications.success.mockReset();
    notifications.error.mockReset();
    TestBed.configureTestingModule({
      imports: [SavedQueriesControlComponent],
      providers: [
        { provide: SavedQueriesApi, useValue: api },
        { provide: NotificationService, useValue: notifications },
      ],
    });
    fixture = TestBed.createComponent(SavedQueriesControlComponent);
    component = fixture.componentInstance;
    component.scope = 'ENTRADAS';
    component.criteria = query.criterios;
    internals = component as unknown as ControlInternals;
    fixture.detectChanges();
  });

  it('carga presets del alcance actual y permite aplicarlos', () => {
    internals.toggleList();
    expect(api.list).toHaveBeenCalledWith('ENTRADAS');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Entradas octubre');
    const applied = vi.spyOn(component.applyCriteria, 'emit');
    internals.apply(query);
    expect(applied).toHaveBeenCalledWith(query.criterios);
  });

  it('rechaza presets obsoletos sin aplicar criterios parcialmente', () => {
    const applied = vi.spyOn(component.applyCriteria, 'emit');
    const stale = { ...query, criterios: { from: { nested: true } } } as unknown as SavedQuery;
    internals.apply(stale);
    expect(applied).not.toHaveBeenCalled();
    expect(notifications.error).toHaveBeenCalledWith('Esta consulta guardada ya no es compatible con esta pantalla.');
  });

  it('crea y actualiza con snapshot de filtros y metadatos actuales', () => {
    internals.openEditor();
    internals.name = '  Guardada  ';
    internals.description = '  Descripción  ';
    internals.save();
    expect(api.create).toHaveBeenCalledWith({ nombre: 'Guardada', descripcion: 'Descripción', alcance: 'ENTRADAS', criterios: query.criterios });

    internals.openEditor(query);
    internals.name = 'Actualizada';
    internals.description = '';
    internals.save();
    expect(api.update).toHaveBeenCalledWith(7, { nombre: 'Actualizada', descripcion: null, alcance: 'ENTRADAS', criterios: query.criterios });
  });

  it('muestra mensaje accionable para duplicado y confirma eliminación', () => {
    api.create.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 409, error: { code: 'RECURSO_DUPLICADO' } })));
    internals.openEditor();
    internals.name = 'Duplicada';
    internals.save();
    expect(notifications.error).toHaveBeenCalledWith('Ya existe una consulta guardada con ese nombre.');

    internals.remove(query);
    expect(confirm).toHaveBeenCalled();
    expect(api.delete).toHaveBeenCalledWith(7);
  });
});
