import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { map, Observable } from 'rxjs';
import { ConfirmDialogComponent, ConfirmDialogData } from './confirm-dialog.component';

/** Abre confirmaciones visuales consistentes para acciones sensibles. */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly dialog = inject(MatDialog);

  ask(data: ConfirmDialogData): Observable<boolean> {
    return this.dialog
      .open(ConfirmDialogComponent, {
        data,
        width: 'min(92vw, 420px)',
        maxWidth: 'calc(100vw - 32px)',
        autoFocus: 'dialog',
        restoreFocus: true,
        ariaLabel: data.title,
      })
      .afterClosed()
      .pipe(map((result) => result === true));
  }
}
