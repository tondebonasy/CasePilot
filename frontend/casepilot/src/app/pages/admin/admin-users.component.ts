import { apiErrorMessage } from '../../core/api-error';
import { Component, inject, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgFor, NgIf } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { firstValueFrom } from 'rxjs';
import { API_BASE } from '../../core/api';

interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  role: string;
  enabled: boolean;
}

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    ReactiveFormsModule, NgFor, NgIf,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatTableModule, MatIconModule, MatSidenavModule,
  ],
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent implements OnInit {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);

  dataSource = new MatTableDataSource<AdminUser>();
  columns = ['name', 'email', 'role', 'status', 'actions'];

  roles = [
    'ADMIN',
    'INTAKE_OFFICER',
    'REVIEWER',
    'EVIDENCE_OFFICER',
    'NETWORK_LIAISON',
    'RESOLUTION_OFFICER',
    'AGENT',
    'COMPLIANCE'
  ];

  roleEdits: Record<number, string> = {};
  drawerOpen = false;
  drawerMode: 'create' | 'reset-password' = 'create';
  selectedUser?: AdminUser;

  userForm = this.fb.group({
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['INTAKE_OFFICER', Validators.required]
  });

  resetPasswordForm = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  loading = true;
  loadFailed = false;
  saving = false;
  message = '';
  error = '';

  ngOnInit() {
    this.loadUsers();
  }

  async loadUsers() {
    this.loadFailed = false;
    this.error = '';
    this.loading = true;
    this.error = '';
    try {
      const users = await firstValueFrom(this.http.get<AdminUser[]>(`${API_BASE}/api/v1/admin/users`));
      this.dataSource.data = users;
      this.roleEdits = {};
    } catch (e) {
      this.loadFailed = true;
      this.error = apiErrorMessage(e, 'load users');
    } finally {
      this.loading = false;
    }
  }

  async createUser() {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.message = '';
    this.error = '';
    try {
      const raw = this.userForm.value;
      const payload = {
        ...raw,
        email: raw.email?.trim(),
        fullName: raw.fullName?.trim()
      };
      await firstValueFrom(this.http.post(`${API_BASE}/api/v1/admin/users`, payload));
      this.message = 'User created successfully.';
      this.userForm.reset({ role: 'INTAKE_OFFICER' });
      await this.loadUsers();
      this.closeDrawer();
    } catch (e) {
      this.error = 'Unable to create user. Please check the form and try again.';
    } finally {
      this.saving = false;
    }
  }

  setRole(user: AdminUser, role: string) {
    this.roleEdits[user.id] = role;
  }

  async saveRole(user: AdminUser) {
    const nextRole = this.roleEdits[user.id] ?? user.role;
    if (nextRole === user.role) {
      return;
    }
    try {
      await firstValueFrom(this.http.put(`${API_BASE}/api/v1/admin/users/${user.id}/role`, { role: nextRole }));
      await this.loadUsers();
    } catch (e) {
      this.error = 'Unable to update role.';
    }
  }

  async toggleStatus(user: AdminUser) {
    try {
      await firstValueFrom(
        this.http.put(`${API_BASE}/api/v1/admin/users/${user.id}/status`, { enabled: !user.enabled })
      );
      await this.loadUsers();
    } catch (e) {
      this.error = 'Unable to update user status.';
    }
  }

  openCreateDrawer() {
    this.drawerMode = 'create';
    this.selectedUser = undefined;
    this.error = '';
    this.message = '';
    this.userForm.reset({ role: 'INTAKE_OFFICER' });
    this.drawerOpen = true;
  }

  openResetPasswordDrawer(user: AdminUser) {
    this.drawerMode = 'reset-password';
    this.selectedUser = user;
    this.error = '';
    this.message = '';
    this.resetPasswordForm.reset();
    this.drawerOpen = true;
  }

  closeDrawer() {
    this.drawerOpen = false;
    this.selectedUser = undefined;
    this.resetPasswordForm.reset();
  }

  async resetPassword() {
    if (!this.selectedUser) {
      return;
    }
    if (this.resetPasswordForm.invalid) {
      this.resetPasswordForm.markAllAsTouched();
      return;
    }
    try {
      const next = this.resetPasswordForm.value.password?.trim();
      await firstValueFrom(
        this.http.put(`${API_BASE}/api/v1/admin/users/${this.selectedUser.id}/password`, { password: next })
      );
      this.message = 'Password reset successfully.';
      this.error = '';
      this.closeDrawer();
    } catch (e) {
      this.error = 'Unable to reset password.';
    }
  }

  label(value: string): string {
    if (!value) return '';
    const cleaned = value.replace(/_/g, ' ').toLowerCase();
    return cleaned.replace(/\b\w/g, (c) => c.toUpperCase());
  }
}
