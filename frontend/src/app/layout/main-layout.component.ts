import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../core/auth/auth.service';
import { ConfirmService } from '../core/ui/confirm-dialog/confirm.service';
import { SidebarComponent } from './navigation/sidebar.component';

/** Shell autenticado: navegación, encabezado y contenido de la aplicación. */
@Component({
  imports: [MatButtonModule, MatIconModule, MatMenuModule, MatToolbarModule, RouterOutlet, SidebarComponent],
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
        (dashboardRequested)="goToDashboard()"
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
          <div class="app-toolbar-context hidden sm:flex" aria-label="Ubicación actual">
            <span class="app-toolbar-context__brand">Anexo 24</span>
            <span class="app-toolbar-context__separator" aria-hidden="true">/</span>
            <strong>{{ currentContext() }}</strong>
          </div>
          <span class="flex-1"></span>
          <button type="button" [matMenuTriggerFor]="userMenu" class="inline-flex h-9.5 items-center gap-2 rounded-lg border-0 bg-transparent px-2.5 text-white transition hover:bg-white/8" aria-label="Abrir menú de usuario">
            <span class="user-initials user-initials-toolbar" aria-hidden="true">{{ auth.initials() }}</span>
            <span class="hidden max-w-40 overflow-hidden text-ellipsis whitespace-nowrap text-xs font-semibold leading-none sm:inline">{{ auth.userName() || 'Usuario' }}</span>
            <mat-icon class="h-4 w-4 shrink-0 text-[18px] text-slate-400" aria-hidden="true">expand_more</mat-icon>
          </button>
          <mat-menu #userMenu="matMenu" xPosition="before">
            <div class="user-menu-header" role="presentation">
              <span class="user-initials user-menu-avatar" aria-hidden="true">{{ auth.initials() }}</span>
              <div><strong>{{ auth.userName() || 'Usuario' }}</strong><span>Sesión activa</span></div>
            </div>
            <div class="user-menu-divider" role="presentation"></div>
            <button mat-menu-item type="button" (click)="goToDashboard()"><mat-icon>space_dashboard</mat-icon><span>Ir al inicio</span></button>
            <button mat-menu-item type="button" (click)="logout()"><mat-icon>logout</mat-icon><span>Cerrar sesión</span></button>
          </mat-menu>
        </mat-toolbar>

        <main class="min-w-0 max-w-full flex-1 overflow-auto"><router-outlet /></main>
      </section>
    </div>
  `,
})
export class MainLayoutComponent {
  protected readonly auth = inject(AuthService);
  protected readonly isMobile = signal(false);
  protected readonly mobileSidebarOpen = signal(false);
  protected readonly sidebarCollapsed = signal(false);
  protected readonly currentContext = signal('Inicio');

  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.currentContext.set(this.contextForUrl(this.router.url));

    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((event) => this.currentContext.set(this.contextForUrl(event.urlAfterRedirects)));

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

  private contextForUrl(url: string): string {
    if (url.includes('/materiales')) return 'Catálogos / Materiales';
    if (url.includes('/dashboard')) return 'Inicio';
    return 'Aplicación';
  }

  protected goToDashboard(): void {
    this.router.navigate(['/dashboard']);
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
