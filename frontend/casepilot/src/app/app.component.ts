import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NgIf, TitleCasePipe } from '@angular/common';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { AuthStore } from './core/auth.store';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    NgIf,
    TitleCasePipe,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  private static readonly SIDEBAR_STATE_KEY = 'casepilot.sidebar.collapsed';

  auth = inject(AuthStore);
  private router = inject(Router);
  sidebarCollapsed = false;

  isAuthed = computed(() => this.auth.isAuthenticated());
  isAdmin = computed(() => this.auth.user()?.role === 'ADMIN');
  userLabel = computed(() => {
    const u = this.auth.user();
    return u ? u.fullName : '';
  });

  constructor() {
    if (typeof window !== 'undefined') {
      this.sidebarCollapsed = window.localStorage.getItem(AppComponent.SIDEBAR_STATE_KEY) === 'true';
    }
  }

  toggleSidebar() {
    this.sidebarCollapsed = !this.sidebarCollapsed;
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(AppComponent.SIDEBAR_STATE_KEY, String(this.sidebarCollapsed));
    }
  }

  logout() {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
