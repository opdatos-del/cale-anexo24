import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, DestroyRef, EventEmitter, Output, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';

export interface OperationPeriod {
  start: Date | null;
  end: Date | null;
}

type Preset = 'today' | 'last7Days' | 'last30Days' | 'currentMonth' | 'previousMonth' | 'currentYear';

/** Selector compacto de periodo para consultas operativas. */
@Component({
  imports: [MatButtonModule, MatDatepickerModule, MatFormFieldModule, MatIconModule, MatMenuModule, ReactiveFormsModule],
  providers: [provideNativeDateAdapter(), { provide: MAT_DATE_LOCALE, useValue: 'es-MX' }],
  selector: 'app-operation-period-filter',
  template: `
    <div class="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-center">
      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="w-full sm:w-96 sm:max-w-full">
        <mat-date-range-input [formGroup]="range" [rangePicker]="picker" separator="—" aria-label="Periodo">
          <input matStartDate formControlName="start" placeholder="Fecha inicial" aria-label="Fecha inicial del periodo" />
          <input matEndDate formControlName="end" placeholder="Fecha final" aria-label="Fecha final del periodo" />
        </mat-date-range-input>
        <mat-datepicker-toggle matIconSuffix [for]="picker" aria-label="Abrir calendario"></mat-datepicker-toggle>
        <mat-date-range-picker #picker [touchUi]="touchUi()"></mat-date-range-picker>
        @if (range.controls.start.hasError('matStartDateInvalid')) {
          <mat-error>Fecha inicial no válida.</mat-error>
        } @else if (range.controls.end.hasError('matEndDateInvalid')) {
          <mat-error>Fecha final no válida.</mat-error>
        } @else if (hasInvalidRange()) {
          <mat-error>La fecha inicial no puede ser posterior a la final.</mat-error>
        }
      </mat-form-field>

      <div class="flex flex-wrap items-center gap-1.5" aria-label="Periodos rápidos">
        <button type="button" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/30" (click)="applyPreset('today')">Hoy</button>
        <button type="button" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/30" (click)="applyPreset('last7Days')">7 días</button>
        <button type="button" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/30" (click)="applyPreset('last30Days')">30 días</button>
        <button type="button" class="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/30" [matMenuTriggerFor]="morePresets" aria-haspopup="menu">
          Más <mat-icon class="h-4 w-4 text-base!" aria-hidden="true">expand_more</mat-icon>
        </button>
      </div>

      <mat-menu #morePresets="matMenu">
        <button mat-menu-item type="button" (click)="applyPreset('currentMonth')"><mat-icon aria-hidden="true">calendar_month</mat-icon><span>Este mes</span></button>
        <button mat-menu-item type="button" (click)="applyPreset('previousMonth')"><mat-icon aria-hidden="true">history</mat-icon><span>Mes anterior</span></button>
        <button mat-menu-item type="button" (click)="applyPreset('currentYear')"><mat-icon aria-hidden="true">event</mat-icon><span>Este año</span></button>
      </mat-menu>
    </div>
  `,
})
export class OperationPeriodFilterComponent {
  @Output() readonly periodChange = new EventEmitter<OperationPeriod>();

  protected readonly range = new FormGroup({
    start: new FormControl<Date | null>(null),
    end: new FormControl<Date | null>(null),
  });
  protected readonly touchUi = signal(false);

  private readonly destroyRef = inject(DestroyRef);
  private readonly breakpointObserver = inject(BreakpointObserver);

  constructor() {
    this.range.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.emitPeriod());
    this.breakpointObserver
      .observe(Breakpoints.Handset)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ matches }) => this.touchUi.set(matches));
  }

  clear(): void {
    this.range.reset({ start: null, end: null }, { emitEvent: false });
  }

  protected applyPreset(preset: Preset): void {
    const today = this.startOfDay(new Date());
    const year = today.getFullYear();
    const month = today.getMonth();
    let start = today;
    let end = today;

    switch (preset) {
      case 'last7Days':
        start = this.addDays(today, -6);
        break;
      case 'last30Days':
        start = this.addDays(today, -29);
        break;
      case 'currentMonth':
        start = new Date(year, month, 1);
        end = new Date(year, month + 1, 0);
        break;
      case 'previousMonth':
        start = new Date(year, month - 1, 1);
        end = new Date(year, month, 0);
        break;
      case 'currentYear':
        start = new Date(year, 0, 1);
        end = new Date(year, 11, 31);
        break;
      case 'today':
        break;
    }

    this.range.setValue({ start, end });
  }

  protected hasInvalidRange(): boolean {
    const { start, end } = this.range.getRawValue();
    return Boolean(start && end && start.getTime() > end.getTime());
  }

  private emitPeriod(): void {
    const { start, end } = this.range.getRawValue();
    this.periodChange.emit({ start, end });
  }

  private startOfDay(value: Date): Date {
    return new Date(value.getFullYear(), value.getMonth(), value.getDate());
  }

  private addDays(value: Date, days: number): Date {
    const result = new Date(value.getFullYear(), value.getMonth(), value.getDate());
    result.setDate(result.getDate() + days);
    return result;
  }
}
