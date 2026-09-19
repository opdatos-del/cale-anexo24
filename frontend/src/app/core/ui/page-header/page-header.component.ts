import { Component, Input } from '@angular/core';

/** Encabezado consistente para páginas de la aplicación. */
@Component({
  selector: 'app-page-header',
  template: `
    <header class="app-page-header">
      <div class="app-page-header__copy">
        @if (eyebrow) {
          <p class="app-page-header__eyebrow">{{ eyebrow }}</p>
        }
        <h1>{{ title }}</h1>
        @if (description) {
          <p class="app-page-header__description">{{ description }}</p>
        }
      </div>
      <div class="app-page-header__actions">
        <ng-content />
      </div>
    </header>
  `,
  styleUrl: './page-header.component.scss',
})
export class PageHeaderComponent {
  @Input() eyebrow = '';
  @Input() title = '';
  @Input() description = '';
}
