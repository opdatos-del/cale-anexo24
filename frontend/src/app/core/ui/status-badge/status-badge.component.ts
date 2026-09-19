import { Component, Input } from '@angular/core';

export type StatusBadgeKind = 'success' | 'info' | 'warning' | 'error' | 'neutral';

/** Indicador compacto para estados operativos. */
@Component({
  selector: 'app-status-badge',
  template: `
    <span class="app-status-badge" [class]="'app-status-badge app-status-badge--' + kind">
      <span class="app-status-badge__dot" aria-hidden="true"></span>
      {{ label }}
    </span>
  `,
  styleUrl: './status-badge.component.scss',
})
export class StatusBadgeComponent {
  @Input() kind: StatusBadgeKind = 'neutral';
  @Input() label = '';
}
