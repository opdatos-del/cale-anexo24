import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';

import { Component, DestroyRef, EventEmitter, Output, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface OperationPeriod {
  start: Date | null;
  end: Date | null;
}

type Preset = 'today' | 'last7Days' | 'last30Days' | 'currentMonth' | 'previousMonth' | 'currentYear';

/** Selector compartido de periodo para consultas operativas. */
@Component({
  imports: [MatDatepickerModule, MatFormFieldModule, ReactiveFormsModule],
  providers: [provideNativeDateAdapter(), { provide: MAT_DATE_LOCALE, useValue: 'es-MX' }],
  selector: 'app-operation-period-filter',
  template: `
    <div class="operation-period-filter">
      <mat-form-field appearance="outline" class="w-full">
        <mat-label>Periodo</mat-label>
        <mat-date-range-input [formGroup]="range" [rangePicker]="picker" separator="—">
          <input matStartDate formControlName="start" placeholder="Fecha inicial" aria-label="Fecha inicial del periodo" />
          <input matEndDate formControlName="end" placeholder="Fecha final" aria-label="Fecha final del periodo" />
        </mat-date-range-input>
        <mat-hint>DD/MM/AAAA – DD/MM/AAAA</mat-hint>
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

      <div class="mt-2 flex flex-wrap items-center gap-2" aria-label="Atajos de periodo">
        <span class="mr-1 text-xs font-medium text-slate-500">Atajos</span>
        <button type="button" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/30" (click)="applyPreset('today')">Hoy</button>
        <button type="button" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/30" (click)="applyPreset('last7Days')">Últimos 7 días</button>
        <button type="button" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/30" (click)="applyPreset('last30Days')">Últimos 30 días</button>
        <button type="button" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/30" (click)="applyPreset('currentMonth')">Este mes</button>
        <button type="button" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/30" (click)="applyPreset('previousMonth')">Mes anterior</button>
        <button type="button" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/30" (click)="applyPreset('currentYear')">Este año</button>
      </div>
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
