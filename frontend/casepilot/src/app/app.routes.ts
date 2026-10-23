import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { adminGuard } from './core/admin.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: 'dashboard', canActivate: [authGuard], loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent) },
  { path: 'disputes', canActivate: [authGuard], loadComponent: () => import('./pages/disputes/disputes.component').then(m => m.DisputesComponent) },
  { path: 'disputes/:id', canActivate: [authGuard], loadComponent: () => import('./pages/dispute-detail/dispute-detail.component').then(m => m.DisputeDetailComponent) },
  { path: 'training', canActivate: [authGuard], loadComponent: () => import('./pages/training/training.component').then(m => m.TrainingComponent) },
  { path: 'training/quiz/:id', canActivate: [authGuard], loadComponent: () => import('./pages/training/quiz-player.component').then(m => m.QuizPlayerComponent) },
  { path: 'admin', canActivate: [authGuard, adminGuard], loadComponent: () => import('./pages/admin/admin.component').then(m => m.AdminComponent) },
  { path: '**', redirectTo: 'dashboard' }
];
