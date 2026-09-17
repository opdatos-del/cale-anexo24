import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
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

/** Shell autenticado persistente: navegación lateral, encabezado y contenido. */
@Component({
  imports: [
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatMenuModule,
    MatSidenavModule,
    MatToolbarModule,
    MatTooltipModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
  ],
  selector: 'app-main-layout',
  styleUrl: './main-layout.component.scss',
  template: `
    <mat-sidenav-container autosize class="h-screen w-full max-w-full overflow-hidden bg-slate-50">
      <mat-sidenav
        #sidenav
        [mode]="isMobile() ? 'over' : 'side'"
        [opened]="isMobile() ? mobileSidenavOpen() : true"
        (openedChange)="mobileSidenavOpen.set($event)"
        [class.compacta]="sidebarCollapsed() && !isMobile()"
        class="sidebar-shell border-r border-slate-200 bg-slate-50"
        aria-label="Navegación principal"
      >
        <div class="flex h-11 items-center border-b border-slate-200 px-3 text-sm font-medium text-slate-700">
          <mat-icon class="shrink-0 text-[20px] text-blue-700">inventory_2</mat-icon>
          <span class="sidebar-copy ml-2">Anexo 24</span>
          @if (!isMobile()) {
            <button
              mat-icon-button
              type="button"
              class="ml-auto !h-8 !w-8 !shrink-0"
              [matTooltip]="sidebarCollapsed() ? 'Expandir menú lateral' : 'Contraer menú lateral'"
              [attr.aria-label]="sidebarCollapsed() ? 'Expandir menú lateral' : 'Contraer menú lateral'"
              [attr.aria-expanded]="!sidebarCollapsed()"
              aria-controls="sidebar-navigation"
              (click)="toggleSidebar()"
            >
              <mat-icon class="!text-[20px]">{{ sidebarCollapsed() ? 'chevron_right' : 'chevron_left' }}</mat-icon>
            </button>
          }
        </div>

        <nav id="sidebar-navigation" mat-nav-list class="px-2 py-3">
          <a
            mat-list-item
            routerLink="/dashboard"
            routerLinkActive="bg-blue-50 text-blue-700"
            [routerLinkActiveOptions]="{ exact: true }"
            matTooltip="Inicio"
            [matTooltipDisabled]="!sidebarCollapsed() || isMobile()"
            [class.justify-center]="sidebarCollapsed() && !isMobile()"
            class="sidebar-nav-item"
            (click)="closeOnMobile(sidenav)"
          >
            <mat-icon matListItemIcon>home</mat-icon>
            <span matListItemTitle class="sidebar-copy">Inicio</span>
          </a>

          @for (group of navigationGroups; track group.label) {
            <div class="sidebar-copy px-3 pb-1 pt-5 text-xs font-medium uppercase tracking-wide text-blue-700">{{ group.label }}</div>
            @for (item of group.items; track item.route) {
              <a
                mat-list-item
                [routerLink]="item.route"
                routerLinkActive="bg-blue-50 text-blue-700"
                [matTooltip]="item.label"
                [matTooltipDisabled]="!sidebarCollapsed() || isMobile()"
                [class.justify-center]="sidebarCollapsed() && !isMobile()"
                class="sidebar-nav-item"
                (click)="closeOnMobile(sidenav)"
              >
                <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
                <span matListItemTitle class="sidebar-copy">{{ item.label }}</span>
              </a>
            }
          }
        </nav>
      </mat-sidenav>

      <mat-sidenav-content class="flex min-w-0 max-w-full flex-col overflow-x-hidden bg-[#f8f9fc]">
        <mat-toolbar class="!h-11 !min-h-11 !bg-[#202428] !px-4 !text-white shadow-sm">
          @if (isMobile()) {
            <button mat-icon-button type="button" aria-label="Abrir menú" (click)="sidenav.toggle()">
              <mat-icon>menu</mat-icon>
            </button>
          }
          <span class="hidden text-sm font-medium sm:inline">ANEXO 24 · Control de Inventarios</span>
          <span class="flex-1"></span>
          <button
            mat-button
            type="button"
            [matMenuTriggerFor]="usuarioMenu"
            class="!min-w-0 !px-2 !text-xs !text-white"
            aria-label="Abrir menú de usuario"
          >
            <mat-icon class="mr-1 !text-[18px]">account_circle</mat-icon>
            <span class="hidden sm:inline">{{ auth.userName() || 'Usuario' }}</span>
            <mat-icon class="ml-1 !text-[16px]">expand_more</mat-icon>
          </button>
          <mat-menu #usuarioMenu="matMenu" xPosition="before">
            <button mat-menu-item type="button" (click)="logout()">
              <mat-icon>logout</mat-icon>
              <span>Cerrar sesión</span>
            </button>
          </mat-menu>
        </mat-toolbar>

        <main class="min-w-0 max-w-full flex-1 overflow-auto">
          <router-outlet />
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
})
export class MainLayoutComponent {
  protected readonly auth = inject(AuthService);
  protected readonly isMobile = signal(false);
  protected readonly mobileSidenavOpen = signal(false);
  protected readonly sidebarCollapsed = signal(false);
  protected readonly navigationGroups: NavGroup[] = [
    {
      label: 'Catálogos',
      items: [{ label: 'Materiales', icon: 'inventory_2', route: '/materiales' }],
    },
  ];

  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    inject(BreakpointObserver)
      .observe(Breakpoints.Handset)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ matches }) => this.isMobile.set(matches));
  }

  protected closeOnMobile(sidenav: MatSidenav): void {
    // El modo overlay se cierra al navegar; desktop conserva sidebar visible.
    if (this.isMobile()) {
      sidenav.close();
    }
  }

  protected toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  protected logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
