import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  kind?: 'warning' | 'danger';
}

/** Diálogo consistente para confirmar acciones sensibles. */
@Component({
  imports: [MatDialogModule, MatIconModule],
  selector: 'app-confirm-dialog',
  styleUrl: './confirm-dialog.component.scss',
  template: `
    <div class="confirm-dialog" [class.confirm-dialog--danger]="data.kind === 'danger'">
      <div class="confirm-dialog__icon" aria-hidden="true">
        <mat-icon>{{ data.kind === 'danger' ? 'delete_outline' : 'logout' }}</mat-icon>
      </div>
      <div mat-dialog-title class="confirm-dialog__title">{{ data.title }}</div>
      <div mat-dialog-content class="confirm-dialog__content">{{ data.message }}</div>
      <div mat-dialog-actions align="end" class="confirm-dialog__actions">
        <button mat-button type="button" class="confirm-dialog__cancel" [mat-dialog-close]="false">{{ data.cancelLabel || 'Cancelar' }}</button>
        <button mat-flat-button type="button" class="confirm-dialog__confirm" [mat-dialog-close]="true">
          {{ data.confirmLabel || 'Confirmar' }}
        </button>
      </div>
    </div>
  `,
})
export class ConfirmDialogComponent {
  protected readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}
