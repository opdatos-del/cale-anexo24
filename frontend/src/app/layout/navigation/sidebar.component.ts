
import { Component, ElementRef, EventEmitter, Input, OnDestroy, Output, ViewChild, computed, inject } from '@angular/core';

import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { createMorph } from 'morphicons/dom';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  permission: string;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

/** Navegación principal responsive con modo compacto y menú de usuario. */
@Component({
  imports: [MatIconModule, MatMenuModule, MatTooltipModule, RouterLink, RouterLinkActive],
  selector: 'app-sidebar',
  styleUrl: './sidebar.component.scss',
  template: `
    <aside
      id="main-navigation"
      class="app-sidebar"
      [class.sidebar-compact]="collapsed && !isMobile"
      [class.mobile-open]="mobileOpen"
      aria-label="Navegación principal"
    >
      <div class="sidebar-brand">
        <span class="sidebar-copy brand-copy">
          <strong class="brand-name">Anexo 24</strong>
          <small>Control de inventarios</small>
        </span>
        @if (!isMobile) {
          <button
            type="button"
            class="sidebar-toggle"
            [matTooltip]="collapsed ? 'Expandir menú lateral' : 'Contraer menú lateral'"
            [attr.aria-label]="collapsed ? 'Expandir menú lateral' : 'Contraer menú lateral'"
            [attr.aria-expanded]="!collapsed"
            aria-controls="main-navigation"
            (click)="toggleSidebar()"
          >
            <svg class="sidebar-toggle-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path #collapsePath d="M19 12H5M12 19L5 12L12 5" />
            </svg>
          </button>
        }
      </div>

      <nav class="sidebar-nav" aria-label="Secciones de la aplicación">
        <a
          routerLink="/dashboard"
          routerLinkActive="nav-active"
          [routerLinkActiveOptions]="{ exact: true }"
          matTooltip="Inicio"
          [matTooltipDisabled]="!collapsed || isMobile"
          class="sidebar-nav-item"
          (click)="closeOnMobile()"
        >
          <mat-icon class="nav-icon" aria-hidden="true">home</mat-icon>
          <span class="sidebar-copy">Inicio</span>
        </a>

        @for (group of navigationGroups(); track group.label) {
          <div class="sidebar-section-label sidebar-copy" [attr.aria-hidden]="collapsed && !isMobile ? 'true' : null">{{ group.label }}</div>
          @for (item of group.items; track item.route) {
            <a
              [routerLink]="item.route"
              routerLinkActive="nav-active"
              [matTooltip]="item.label"
              [matTooltipDisabled]="!collapsed || isMobile"
              class="sidebar-nav-item"
              (click)="closeOnMobile()"
            >
              <mat-icon class="nav-icon" aria-hidden="true">{{ item.icon }}</mat-icon>
              <span class="sidebar-copy">{{ item.label }}</span>
            </a>
          }
        }
      </nav>

      <div class="sidebar-footer">
        <button type="button" class="sidebar-user" [matMenuTriggerFor]="userMenu" aria-label="Abrir menú de usuario">
          <span class="user-initials sidebar-user-avatar" aria-hidden="true">{{ auth.initials() }}</span>
          <span class="sidebar-copy sidebar-user-copy">
            <strong>{{ auth.userName() || 'Usuario' }}</strong>
            <small><span class="session-dot" aria-hidden="true"></span>Sesión activa</small>
          </span>
          <mat-icon class="sidebar-user-chevron" aria-hidden="true">more_vert</mat-icon>
        </button>
      </div>
    </aside>

    <mat-menu #userMenu="matMenu" xPosition="after">
      <div class="user-menu-header" role="presentation">
        <span class="user-initials user-menu-avatar" aria-hidden="true">{{ auth.initials() }}</span>
        <div><strong>{{ auth.userName() || 'Usuario' }}</strong><span>Sesión activa</span></div>
      </div>
      <div class="user-menu-divider" role="presentation"></div>
      <button mat-menu-item type="button" (click)="dashboardRequested.emit()"><mat-icon>space_dashboard</mat-icon><span>Ir al inicio</span></button>
      <button mat-menu-item type="button" (click)="logoutRequested.emit()"><mat-icon>logout</mat-icon><span>Cerrar sesión</span></button>
    </mat-menu>
  `,
})
export class SidebarComponent implements OnDestroy {
  @ViewChild('collapsePath')
  private set collapsePath(path: ElementRef<SVGPathElement> | undefined) {
    this.collapseMorph?.destroy();
    this.collapseMorph = path ? createMorph(path.nativeElement, this.arrowPath, { reducedMotion: 'user' }) : undefined;
  }

  @Input() collapsed = false;
  @Input() isMobile = false;
  @Input() mobileOpen = false;
  @Output() readonly collapsedChange = new EventEmitter<boolean>();
  @Output() readonly mobileClosed = new EventEmitter<void>();
  @Output() readonly dashboardRequested = new EventEmitter<void>();
  @Output() readonly logoutRequested = new EventEmitter<void>();

  protected readonly auth = inject(AuthService);
  protected readonly navigationGroups = computed<NavGroup[]>(() => {
    const groups: NavGroup[] = [];
    const catalogItems: NavItem[] = [
      { label: 'Materiales', icon: 'inventory_2', route: '/materiales', permission: 'MATERIALES_CONSULTAR' },
      { label: 'Productos', icon: 'category', route: '/productos', permission: 'PRODUCTOS_CONSULTAR' },
      { label: 'Estructuras', icon: 'account_tree', route: '/estructuras', permission: 'ESTRUCTURAS_CONSULTAR' },
    ].filter((item) => this.auth.hasPermission(item.permission));

    if (catalogItems.length > 0) groups.push({ label: 'Catálogos', items: catalogItems });

    if (this.auth.hasPermission('OPERACIONES_CONSULTAR')) {
      groups.push({
        label: 'Operaciones',
        items: [
          { label: 'Entradas', icon: 'move_to_inbox', route: '/operaciones/entradas', permission: 'OPERACIONES_CONSULTAR' },
          { label: 'Salidas', icon: 'outbox', route: '/operaciones/salidas', permission: 'OPERACIONES_CONSULTAR' },
        ],
      });
    }

    return groups;
  });

  private readonly menuPath = 'M4 6H20M4 12H20M4 18H20';
  private readonly arrowPath = 'M19 12H5M12 19L5 12L12 5';
  private collapseMorph?: ReturnType<typeof createMorph>;


  ngOnDestroy(): void {
    this.collapseMorph?.destroy();
  }

  protected toggleSidebar(): void {
    const nextCollapsed = !this.collapsed;
    this.collapseMorph?.morphTo(nextCollapsed ? this.menuPath : this.arrowPath, 'smooth');
    this.collapsedChange.emit(nextCollapsed);
  }

  protected closeOnMobile(): void {
    if (this.isMobile) this.mobileClosed.emit();
  }
}
