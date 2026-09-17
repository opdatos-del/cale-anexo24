import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

/** Shell autenticado: navegación, encabezado y contenido de la aplicación. */
@Component({
  imports: [MatButtonModule, MatIconModule, MatMenuModule, MatToolbarModule, MatTooltipModule, RouterLink, RouterLinkActive, RouterOutlet],
  selector: 'app-main-layout',
  styleUrl: './main-layout.component.scss',
  template: `
    <div class="app-shell" [class.sidebar-compact]="sidebarCollapsed() && !isMobile()">
      @if (isMobile() && mobileSidebarOpen()) {
        <button class="mobile-backdrop" type="button" aria-label="Cerrar menú" (click)="closeMobileSidebar()"></button>
      }

      <aside class="app-sidebar" [class.mobile-open]="mobileSidebarOpen()" aria-label="Navegación principal">
        <div class="sidebar-brand">
          <span class="brand-mark"><mat-icon>inventory_2</mat-icon></span>
          <span class="sidebar-copy brand-name">Anexo 24</span>
          @if (!isMobile()) {
            <button
              type="button"
              class="sidebar-toggle"
              [matTooltip]="sidebarCollapsed() ? 'Expandir menú lateral' : 'Contraer menú lateral'"
              [attr.aria-label]="sidebarCollapsed() ? 'Expandir menú lateral' : 'Contraer menú lateral'"
              [attr.aria-expanded]="!sidebarCollapsed()"
              (click)="toggleSidebar()"
            >
              <mat-icon>{{ sidebarCollapsed() ? 'chevron_right' : 'chevron_left' }}</mat-icon>
            </button>
          }
        </div>

        <nav class="sidebar-nav" aria-label="Secciones de la aplicación">
          <a
            routerLink="/dashboard"
            routerLinkActive="nav-active"
            [routerLinkActiveOptions]="{ exact: true }"
            matTooltip="Inicio"
            [matTooltipDisabled]="!sidebarCollapsed() || isMobile()"
            class="sidebar-nav-item"
            (click)="closeOnMobile()"
          >
            <mat-icon class="nav-icon">home</mat-icon>
            <span class="sidebar-copy">Inicio</span>
          </a>

          @for (group of navigationGroups; track group.label) {
            <div class="sidebar-section-label sidebar-copy">{{ group.label }}</div>
            @for (item of group.items; track item.route) {
              <a
                [routerLink]="item.route"
                routerLinkActive="nav-active"
                [matTooltip]="item.label"
                [matTooltipDisabled]="!sidebarCollapsed() || isMobile()"
                class="sidebar-nav-item"
                (click)="closeOnMobile()"
              >
                <mat-icon class="nav-icon">{{ item.icon }}</mat-icon>
                <span class="sidebar-copy">{{ item.label }}</span>
              </a>
            }
          }
        </nav>
      </aside>

      <section class="app-content">
        <mat-toolbar class="app-toolbar">
          @if (isMobile()) {
            <button mat-icon-button type="button" aria-label="Abrir menú" (click)="toggleMobileSidebar()"><mat-icon>menu</mat-icon></button>
          }
          <span class="hidden text-[13px] font-medium tracking-wide text-slate-200 sm:inline">ANEXO 24 <span class="mx-1 text-slate-500">·</span> Control de Inventarios</span>
          <span class="flex-1"></span>
          <button type="button" [matMenuTriggerFor]="userMenu" class="inline-flex h-[38px] items-center gap-2 rounded-lg border-0 bg-transparent px-2.5 text-white transition hover:bg-white/8" aria-label="Abrir menú de usuario">
            <img class="h-[26px] w-[26px] shrink-0 rounded-full bg-blue-100 object-cover" [src]="auth.avatarUrl()" alt="" />
            <span class="hidden max-w-40 overflow-hidden text-ellipsis whitespace-nowrap text-xs font-semibold leading-none sm:inline">{{ auth.userName() || 'Usuario' }}</span>
            <mat-icon class="h-4 w-4 shrink-0 text-[18px] text-slate-400">expand_more</mat-icon>
          </button>
          <mat-menu #userMenu="matMenu" xPosition="before">
            <div class="user-menu-header" role="presentation">
              <img class="user-menu-avatar" [src]="auth.avatarUrl()" alt="" />
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
  protected readonly navigationGroups: NavGroup[] = [{ label: 'Catálogos', items: [{ label: 'Materiales', icon: 'inventory_2', route: '/materiales' }] }];

  private readonly router = inject(Router);
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

  protected closeOnMobile(): void {
    if (this.isMobile()) this.mobileSidebarOpen.set(false);
  }

  protected toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  protected toggleMobileSidebar(): void {
    this.mobileSidebarOpen.update((open) => !open);
  }

  protected closeMobileSidebar(): void {
    this.mobileSidebarOpen.set(false);
  }

  protected goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  protected logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
