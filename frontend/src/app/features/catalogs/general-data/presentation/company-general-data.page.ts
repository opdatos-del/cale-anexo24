import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AppAlertComponent } from '@core/ui/app-alert/app-alert.component';
import { NotificationService } from '@core/notifications/notification.service';
import { userFacingApiError } from '@core/http/api-error.util';
import { CompanyGeneralData } from '@features/catalogs/general-data/domain/company-general-data.model';
import { CompanyGeneralDataApi } from '@features/catalogs/general-data/infrastructure/company-general-data.api';

/** Ficha read-only de datos generales de la empresa. */
@Component({
  imports: [AppAlertComponent, MatButtonModule, MatIconModule],
  selector: 'app-company-general-data',
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800">
      <main class="mx-auto w-full max-w-360 px-5 py-8 sm:px-8">
        <div class="mb-7">
          <p class="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Catálogos</p>
          <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Datos generales de la empresa</h1>
          <p class="mt-2 text-sm text-slate-500">Consulta read-only de la ficha empresarial. La edición no forma parte de esta versión.</p>
        </div>

        @if (isLoading()) {
          <section class="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm" aria-label="Cargando datos generales" aria-busy="true">
            @for (row of loadingRows; track row) { <div class="h-12 animate-pulse rounded-lg bg-slate-100"></div> }
          </section>
        } @else if (error()) {
          <app-alert kind="error" title="No pudimos cargar los datos generales" [message]="error()!" actionLabel="Reintentar" (action)="load()" />
        } @else if (!data()) {
          <app-alert kind="info" title="No hay datos generales disponibles" message="La fuente empresarial no tiene un registro para mostrar." actionLabel="Reintentar" (action)="load()" />
        } @else {
          <section class="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-labelledby="company-data-title">
            <h2 id="company-data-title" class="mb-6 text-lg font-semibold text-slate-900">Información empresarial</h2>
            <dl class="grid gap-5 sm:grid-cols-2">
              <div><dt class="text-xs font-medium uppercase tracking-wide text-slate-500">Razón social</dt><dd class="mt-1 text-sm text-slate-900">{{ data()!.razonSocial || 'No disponible' }}</dd></div>
              <div><dt class="text-xs font-medium uppercase tracking-wide text-slate-500">RFC</dt><dd class="mt-1 text-sm text-slate-900">{{ data()!.rfc || 'No disponible' }}</dd></div>
              <div><dt class="text-xs font-medium uppercase tracking-wide text-slate-500">Registro IMMEX</dt><dd class="mt-1 text-sm text-slate-900">{{ data()!.registroImmex || 'No disponible' }}</dd></div>
              <div class="sm:col-span-2"><dt class="text-xs font-medium uppercase tracking-wide text-slate-500">Domicilio fiscal</dt><dd class="mt-1 whitespace-pre-line text-sm text-slate-900">{{ data()!.domicilioFiscal || 'No disponible' }}</dd></div>
            </dl>
            <div class="mt-7 border-t border-slate-100 pt-5">
              <button mat-stroked-button type="button" class="rounded-xl!" (click)="load()"><mat-icon>refresh</mat-icon>Actualizar consulta</button>
            </div>
          </section>
        }
      </main>
    </div>
  `,
})
export class CompanyGeneralDataPage implements OnInit {
  protected readonly loadingRows = [1, 2, 3, 4];
  protected readonly data = signal<CompanyGeneralData | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly api = inject(CompanyGeneralDataApi);
  private readonly notifications = inject(NotificationService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.api.get().subscribe({
      next: (response) => {
        this.data.set(response);
        this.isLoading.set(false);
      },
      error: (error: unknown) => {
        this.isLoading.set(false);
        this.error.set(userFacingApiError(error, 'Verifica tu conexión e inténtalo nuevamente.'));
        this.notifications.error('No fue posible consultar los datos generales.');
      },
    });
  }
}
