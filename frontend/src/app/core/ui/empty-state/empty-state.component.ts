import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/** Estado vacío orientado a explicar el siguiente paso al usuario. */
@Component({
  imports: [MatButtonModule, MatIconModule],
  selector: 'app-empty-state',
  template: `
    <div class="app-empty-state" role="status">
      <span class="app-empty-state__icon" aria-hidden="true"><mat-icon>{{ icon }}</mat-icon></span>
      <h2>{{ title }}</h2>
      <p>{{ message }}</p>
      @if (actionLabel) {
        <button mat-stroked-button type="button" (click)="action.emit()">
          <mat-icon aria-hidden="true">filter_alt_off</mat-icon>
          {{ actionLabel }}
        </button>
      }
    </div>
  `,
  styleUrl: './empty-state.component.scss',
})
export class EmptyStateComponent {
  @Input() icon = 'search_off';
  @Input() title = 'Sin resultados';
  @Input() message = 'No encontramos información para los criterios seleccionados.';
  @Input() actionLabel = '';
  @Output() readonly action = new EventEmitter<void>();
}
