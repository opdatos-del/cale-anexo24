import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export type AlertKind = 'info' | 'success' | 'warning' | 'error';

/** Alerta inline reutilizable con semántica, icono y acción opcional. */
@Component({
  imports: [MatButtonModule, MatIconModule],
  selector: 'app-alert',
  styleUrl: './app-alert.component.scss',
  template: `
    <div [class]="'app-alert app-alert--' + kind" [attr.role]="kind === 'info' ? 'status' : 'alert'" [attr.aria-live]="kind === 'info' ? 'polite' : 'assertive'">
      <mat-icon class="app-alert__icon" aria-hidden="true">{{ icon }}</mat-icon>
      <div class="app-alert__content">
        @if (title) {
          <p class="app-alert__title">{{ title }}</p>
        }
        <p class="app-alert__message">{{ message }}</p>
      </div>
      @if (actionLabel) {
        <button mat-stroked-button type="button" class="app-alert__action" (click)="action.emit()">{{ actionLabel }}</button>
      }
      @if (dismissible) {
        <button mat-icon-button type="button" class="app-alert__close" aria-label="Cerrar aviso" (click)="closed.emit()">
          <mat-icon aria-hidden="true">close</mat-icon>
        </button>
      }
    </div>
  `,
})
export class AppAlertComponent {
  @Input() kind: AlertKind = 'info';
  @Input() title = '';
  @Input() message = '';
  @Input() actionLabel = '';
  @Input() dismissible = false;
  @Output() readonly action = new EventEmitter<void>();
  @Output() readonly closed = new EventEmitter<void>();

  get icon(): string {
    return {
      info: 'info',
      success: 'check_circle',
      warning: 'warning',
      error: 'error_outline',
    }[this.kind];
  }
}
