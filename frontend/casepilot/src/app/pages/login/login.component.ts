import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { NgIf } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { API_BASE } from '../../core/api';
import { AuthStore } from '../../core/auth.store';

@Component({
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    NgIf,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private router = inject(Router);
  private auth = inject(AuthStore);

  loading = false;
  error = '';
  hidePassword = true;

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  async submit() {
    this.error = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;

    try {
      const raw = this.form.value;
      const payload = {
        ...raw,
        email: raw.email?.trim()
      };
      const res: any = await this.http.post(`${API_BASE}/api/v1/auth/login`, payload).toPromise();
      const token = res.accessToken as string;
      const me: any = await this.http.get(`${API_BASE}/api/v1/users/me`, { headers: { Authorization: `Bearer ${token}` } }).toPromise();

      this.auth.setSession(token, {
        id: me.id,
        email: me.email,
        fullName: me.fullName,
        role: me.role
      });

      this.router.navigateByUrl('/dashboard');
    } catch (e: any) {
      this.error = 'Login failed. Please check your email and password.';
    } finally {
      this.loading = false;
    }
  }
}
