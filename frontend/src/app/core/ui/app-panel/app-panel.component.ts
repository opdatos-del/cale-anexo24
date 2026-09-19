import { Component, Input } from '@angular/core';

/** Superficie reutilizable para agrupar contenido relacionado. */
@Component({
  selector: 'app-panel',
  template: `<section class="app-panel" [class.app-panel--flush]="flush"><ng-content /></section>`,
  styleUrl: './app-panel.component.scss',
})
export class AppPanelComponent {
  @Input() flush = false;
}
