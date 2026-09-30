import { Component, EventEmitter, Input, Output, computed, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { NAVIGATION_GROUPS, NavigationGroup, NavigationItem } from '@layout/navigation/navigation.config';

/** Navegación agrupada con scroll interno, mini rail y cuenta accesible. */
@Component({
  imports: [MatIconModule, MatMenuModule, MatTooltipModule, RouterLink, RouterLinkActive],
  selector: 'app-sidebar', styleUrl: './sidebar.component.scss',
  template: `
    <aside id="main-navigation" class="app-sidebar" [class.sidebar-compact]="collapsed && !isMobile" [class.mobile-open]="mobileOpen" aria-label="Navegación principal">
      <header class="sidebar-brand"><span class="sidebar-copy brand-copy"><strong>Anexo 24</strong><small>Control de inventarios</small></span>
        @if (!isMobile) { <button type="button" class="icon-button" [attr.aria-label]="collapsed ? 'Expandir menú lateral':'Contraer menú lateral'" (click)="collapsedChange.emit(!collapsed)"><mat-icon>{{collapsed?'menu_open':'menu'}}</mat-icon></button> }
      </header>
      <nav class="sidebar-nav" aria-label="Secciones de la aplicación">
        <a routerLink="/dashboard" routerLinkActive="nav-active" [routerLinkActiveOptions]="{exact:true}" class="nav-item root-item" matTooltip="Inicio" [matTooltipDisabled]="!collapsed||isMobile" (click)="navigate()"><mat-icon>home</mat-icon><span class="sidebar-copy">Inicio</span></a>
        @for (group of groups(); track group.label) {
          @if (collapsed && !isMobile) {
            <button type="button" class="nav-item group-button rail-group" [matMenuTriggerFor]="railMenu" [matTooltip]="group.label" [attr.aria-label]="group.label"><mat-icon>{{group.icon}}</mat-icon></button>
            <mat-menu #railMenu="matMenu" xPosition="after">@for(item of group.items; track item.route){<a mat-menu-item [routerLink]="item.route" (click)="navigate()"><mat-icon>{{item.icon}}</mat-icon><span>{{item.label}}</span></a>}</mat-menu>
          } @else {
            <button type="button" class="nav-item group-button" [class.group-active]="groupActive(group)" [attr.aria-expanded]="isExpanded(group.label)" (click)="toggleGroup(group.label)"><mat-icon>{{group.icon}}</mat-icon><span class="sidebar-copy">{{group.label}}</span><mat-icon class="chevron" [class.rotated]="isExpanded(group.label)">expand_more</mat-icon></button>
            @if (isExpanded(group.label)) { <div class="group-children">@for(item of group.items; track item.route){<a [routerLink]="item.route" routerLinkActive="nav-active" class="nav-item child-item" (click)="navigate()"><mat-icon>{{item.icon}}</mat-icon><span>{{item.label}}</span></a>}</div> }
          }
        }
      </nav>
      <footer class="sidebar-footer"><button type="button" class="sidebar-user" [matMenuTriggerFor]="userMenu" aria-label="Abrir menú de usuario"><span class="avatar">{{auth.initials()}}</span><span class="sidebar-copy identity"><strong>{{auth.userName()||'Usuario'}}</strong><small>Sesión activa</small></span><mat-icon class="sidebar-copy">more_vert</mat-icon></button></footer>
    </aside>
    <mat-menu #userMenu="matMenu" xPosition="after"><div class="account-header"><span class="avatar">{{auth.initials()}}</span><div><strong>{{auth.userName()||'Usuario'}}</strong><small>Sesión activa</small></div></div><button mat-menu-item type="button" (click)="logoutRequested.emit()"><mat-icon>logout</mat-icon><span>Cerrar sesión</span></button></mat-menu>
  `,
})
export class SidebarComponent {
  @Input() collapsed=false; @Input() isMobile=false; @Input() mobileOpen=false;
  @Output() readonly collapsedChange=new EventEmitter<boolean>(); @Output() readonly mobileClosed=new EventEmitter<void>(); @Output() readonly logoutRequested=new EventEmitter<void>();
  protected readonly auth=inject(AuthService); private readonly router=inject(Router); private readonly expanded=signal(new Set<string>(NAVIGATION_GROUPS.map(g=>g.label)));
  protected readonly groups=computed(() => NAVIGATION_GROUPS.map(group=>({...group,items:group.items.filter(item=>this.allowed(item))})).filter(group=>group.items.length));
  protected isExpanded(label:string):boolean{return this.expanded().has(label);} protected toggleGroup(label:string):void{this.expanded.update(current=>{const next=new Set(current);if(next.has(label)) next.delete(label); else next.add(label);return next;});}
  protected groupActive(group:NavigationGroup):boolean{return group.items.some(item=>this.router.url===item.route||this.router.url.startsWith(item.route+'/'));}
  protected navigate():void{if(this.isMobile)this.mobileClosed.emit();}
  private allowed(item:NavigationItem):boolean{return item.permission?this.auth.hasPermission(item.permission):!!item.anyOfPermissions?.some(permission=>this.auth.hasPermission(permission));}
}
