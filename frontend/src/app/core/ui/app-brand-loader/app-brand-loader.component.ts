import { Component, Input } from '@angular/core';

/** Overlay de carga reutilizable con la identidad visual de Anexo 24. */
@Component({
  selector: 'app-brand-loader',
  template: `
    @if (visible) {
      <div class="brand-loader-overlay" role="status" aria-live="polite" aria-busy="true">
        <div class="brand-loader-panel">
          <div class="sk-chase" aria-hidden="true">
            <div class="sk-chase-dot"></div>
            <div class="sk-chase-dot"></div>
            <div class="sk-chase-dot"></div>
            <div class="sk-chase-dot"></div>
            <div class="sk-chase-dot"></div>
            <div class="sk-chase-dot"></div>
          </div>
          <span>{{ label }}</span>
        </div>
      </div>
    }
  `,
  styleUrl: './app-brand-loader.component.scss',
})
export class AppBrandLoaderComponent {
  @Input() visible = false;
  @Input() label = 'Cargando...';
}
