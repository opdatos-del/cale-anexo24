import { Injectable, inject } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

export type NotificationType = 'success' | 'error' | 'info' | 'warning';

/** Presenta mensajes breves y consistentes para operaciones de la aplicación. */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.open(message, 'success');
  }

  error(message: string): void {
    this.open(message, 'error', 6000);
  }

  info(message: string): void {
    this.open(message, 'info');
  }

  warning(message: string): void {
    this.open(message, 'warning');
  }

  private open(message: string, type: NotificationType, duration = 4000): void {
    const config: MatSnackBarConfig = {
      duration,
      horizontalPosition: 'right',
      verticalPosition: 'bottom',
      panelClass: [`app-snackbar-${type}`],
    };

    this.snackBar.open(message, 'Cerrar', config);
  }
}
