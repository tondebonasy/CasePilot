import { apiErrorMessage } from '../../core/api-error';
import { Component, inject, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { DatePipe, NgFor, NgIf } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { API_BASE } from '../../core/api';

export interface Policy {
  id: number;
  title: string;
  circularReference: string;
  summary: string;
  effectiveDate: string;
  keyRequirements: string;
  active: boolean;
}

@Component({
  selector: 'app-policy-manager',
  standalone: true,
  imports: [
    ReactiveFormsModule, NgFor, NgIf, DatePipe,
    MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule,
    MatCheckboxModule, MatIconModule, MatSidenavModule,
    MatDatepickerModule, MatNativeDateModule,
  ],
  templateUrl: './policy-manager.component.html',
  styleUrls: ['./policy-manager.component.scss']
})
export class PolicyManagerComponent implements OnInit {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);

  policies: Policy[] = [];
  loading = true;
  loadFailed = false;

  policyForm = this.fb.group({
    title: ['', Validators.required],
    circularReference: ['', Validators.required],
    summary: ['', Validators.required],
    effectiveDate: [null as Date | null, Validators.required],
    keyRequirements: ['', Validators.required],
    active: [true]
  });

  editingPolicyId?: number;
  drawerOpen = false;
  savingPolicy = false;
  policyMessage = '';
  policyError = '';

  ngOnInit() {
    this.loadPolicies();
  }

  async loadPolicies() {
    this.loadFailed = false;
    this.policyError = '';
    this.loading = true;
    try {
      this.policies = await firstValueFrom(this.http.get<Policy[]>(`${API_BASE}/api/v1/admin/training/policies`));
    } catch (error) {
      this.loadFailed = true;
      this.policyError = apiErrorMessage(error, 'load policies');
    } finally {
      this.loading = false;
    }
  }

  editPolicy(policy: Policy) {
    this.editingPolicyId = policy.id;
    this.drawerOpen = true;
    this.policyForm.patchValue({
      ...policy,
      effectiveDate: this.parseLocalDate(policy.effectiveDate)
    });
    this.policyMessage = '';
    this.policyError = '';
  }

  resetPolicyForm() {
    this.editingPolicyId = undefined;
    this.policyForm.reset({ active: true });
    this.policyError = '';
  }

  openCreateDrawer() {
    this.resetPolicyForm();
    this.policyMessage = '';
    this.drawerOpen = true;
  }

  closeDrawer() {
    this.drawerOpen = false;
    this.resetPolicyForm();
  }

  async savePolicy() {
    if (this.policyForm.invalid) {
      this.policyForm.markAllAsTouched();
      return;
    }
    this.savingPolicy = true;
    this.policyError = '';
    this.policyMessage = '';

    try {
      const raw = this.policyForm.getRawValue();
      const payload = {
        ...raw,
        effectiveDate: this.toLocalDateString(raw.effectiveDate)
      };
      if (this.editingPolicyId) {
        await firstValueFrom(this.http.put(`${API_BASE}/api/v1/admin/training/policies/${this.editingPolicyId}`, payload));
        this.policyMessage = 'Policy updated successfully.';
      } else {
        await firstValueFrom(this.http.post(`${API_BASE}/api/v1/admin/training/policies`, payload));
        this.policyMessage = 'New policy created successfully.';
      }
      this.resetPolicyForm();
      await this.loadPolicies();
      this.drawerOpen = false;
    } catch (err) {
      this.policyError = 'Unable to save policy.';
    } finally {
      this.savingPolicy = false;
    }
  }

  async togglePolicyActive(policy: Policy) {
    const payload = { ...policy, active: !policy.active };
    try {
      await firstValueFrom(this.http.put(`${API_BASE}/api/v1/admin/training/policies/${policy.id}`, payload));
      this.policyMessage = `Policy successfully ${payload.active ? 'activated' : 'deactivated'}.`;
      await this.loadPolicies();
    } catch (err) {
      this.policyError = 'Unable to toggle policy state.';
    }
  }

  private parseLocalDate(value: string | null | undefined): Date | null {
    if (!value) {
      return null;
    }

    const [year, month, day] = value.split('-').map(Number);
    if (!year || !month || !day) {
      return null;
    }

    return new Date(year, month - 1, day);
  }

  private toLocalDateString(value: Date | string | null | undefined): string {
    if (!value) {
      return '';
    }
    if (typeof value === 'string') {
      return value.slice(0, 10);
    }

    const year = value.getFullYear();
    const month = `${value.getMonth() + 1}`.padStart(2, '0');
    const day = `${value.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
