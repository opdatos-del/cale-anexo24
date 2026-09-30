import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterOutlet } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { ConfirmService } from '@core/ui/confirm-dialog/confirm.service';
import { SidebarComponent } from '@layout/navigation/sidebar.component';

/** Shell autenticado: navegación, encabezado y contenido de la aplicación. */
@Component({
  imports: [MatButtonModule, MatIconModule, MatToolbarModule, RouterOutlet, SidebarComponent],
  selector: 'app-main-layout',
  styleUrl: './main-layout.component.scss',
  template: `
    <div class="app-shell" [class.sidebar-compact]="sidebarCollapsed() && !isMobile()">
      @if (isMobile() && mobileSidebarOpen()) {
        <button class="mobile-backdrop" type="button" aria-label="Cerrar menú" (click)="closeMobileSidebar()"></button>
      }

      <app-sidebar
        [collapsed]="sidebarCollapsed()"
        [isMobile]="isMobile()"
        [mobileOpen]="mobileSidebarOpen()"
        (collapsedChange)="sidebarCollapsed.set($event)"
        (mobileClosed)="closeMobileSidebar()"
        (logoutRequested)="logout()"
      />

      <section class="app-content">
        <mat-toolbar class="app-toolbar">
          @if (isMobile()) {
            <button
              mat-icon-button
              type="button"
              aria-label="Abrir menú"
              aria-controls="main-navigation"
              [attr.aria-expanded]="mobileSidebarOpen()"
              (click)="toggleMobileSidebar()"
            >
              <mat-icon aria-hidden="true">menu</mat-icon>
            </button>
          }
          <span class="hidden text-[13px] font-medium tracking-wide text-slate-200 sm:inline">ANEXO 24 <span class="mx-1 text-slate-500">·</span> Control de Inventarios</span>
          <span class="flex-1"></span>
        </mat-toolbar>

        <main class="main-content-scroll" data-testid="main-content-scroll"><router-outlet /></main>
      </section>
    </div>
  `,
})
export class MainLayoutComponent {
  protected readonly auth = inject(AuthService);
  protected readonly isMobile = signal(false);
  protected readonly mobileSidebarOpen = signal(false);
  protected readonly sidebarCollapsed = signal(false);

  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    inject(BreakpointObserver)
      .observe(Breakpoints.Handset)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ matches }) => {
        this.isMobile.set(matches);
        if (!matches) this.mobileSidebarOpen.set(false);
      });
  }

  @HostListener('document:keydown.escape')
  protected closeMobileSidebar(): void {
    if (this.isMobile()) this.mobileSidebarOpen.set(false);
  }

  protected toggleMobileSidebar(): void {
    this.mobileSidebarOpen.update((open) => !open);
  }


  protected logout(): void {
    this.confirm
      .ask({
        title: 'Cerrar sesión',
        message: 'La sesión actual se cerrará en este dispositivo. ¿Quieres continuar?',
        confirmLabel: 'Cerrar sesión',
        cancelLabel: 'Seguir aquí',
        kind: 'warning',
      })
      .subscribe((confirmed) => {
        if (!confirmed) return;

        this.auth.logout();
        this.router.navigate(['/login']);
      });
  }
}
