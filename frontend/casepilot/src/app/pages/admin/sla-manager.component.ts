import { apiErrorMessage } from '../../core/api-error';
import { Component, inject, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { NgFor, NgIf } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { API_BASE } from '../../core/api';

export interface SlaRule {
  id: number;
  network: string;
  disputeType: string;
  internalDays: number;
  networkDays: number;
}

@Component({
  selector: 'app-sla-manager',
  standalone: true,
  imports: [
    ReactiveFormsModule, NgFor, NgIf,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatTableModule, MatIconModule, MatSidenavModule,
  ],
  templateUrl: './sla-manager.component.html',
  styleUrls: ['./sla-manager.component.scss']
})
export class SlaManagerComponent implements OnInit {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);

  dataSource = new MatTableDataSource<SlaRule>();
  slaColumns = ['network', 'disputeType', 'internalDays', 'networkDays', 'actions'];

  networks = ['VISA', 'MASTERCARD', 'ZIMSWITCH'];
  disputeTypes = [
    'UNAUTHORIZED', 'DUPLICATE', 'NOT_RECEIVED', 'DEFECTIVE_OR_NOT_AS_DESCRIBED',
    'CANCELLED_RECURRING', 'PROCESSING_ERROR', 'OTHER'
  ];

  slaForm = this.fb.group({
    network: ['VISA', Validators.required],
    disputeType: ['UNAUTHORIZED', Validators.required],
    internalDays: [5, [Validators.required, Validators.min(1)]],
    networkDays: [10, [Validators.required, Validators.min(1)]]
  });

  editingSlaId?: number;
  drawerOpen = false;
  savingSla = false;
  slaMessage = '';
  slaError = '';
  loading = true;
  loadFailed = false;

  ngOnInit() {
    this.loadSlaRules();
  }

  async loadSlaRules() {
    this.loadFailed = false;
    this.slaError = '';
    this.loading = true;
    try {
      const rules = await firstValueFrom(this.http.get<SlaRule[]>(`${API_BASE}/api/v1/admin/sla`));
      this.dataSource.data = rules;
    } catch (error) {
      this.loadFailed = true;
      this.slaError = apiErrorMessage(error, 'load SLA rules');
    } finally {
      this.loading = false;
    }
  }

  editSlaRule(rule: SlaRule) {
    this.editingSlaId = rule.id;
    this.drawerOpen = true;
    this.slaForm.setValue({
      network: rule.network,
      disputeType: rule.disputeType,
      internalDays: rule.internalDays,
      networkDays: rule.networkDays
    });
  }

  resetSlaForm() {
    this.editingSlaId = undefined;
    this.slaForm.reset({
      network: 'VISA',
      disputeType: 'UNAUTHORIZED',
      internalDays: 5,
      networkDays: 10
    });
    this.slaError = '';
  }

  openCreateDrawer() {
    this.resetSlaForm();
    this.slaMessage = '';
    this.drawerOpen = true;
  }

  closeDrawer() {
    this.drawerOpen = false;
    this.resetSlaForm();
  }

  async saveSla() {
    if (this.slaForm.invalid) {
      this.slaForm.markAllAsTouched();
      return;
    }
    this.savingSla = true;
    this.slaError = '';
    this.slaMessage = '';

    try {
      const payload = this.slaForm.value;
      if (this.editingSlaId) {
        await firstValueFrom(this.http.put(`${API_BASE}/api/v1/admin/sla/${this.editingSlaId}`, payload));
        this.slaMessage = 'SLA rule updated successfully.';
      } else {
        await firstValueFrom(this.http.post(`${API_BASE}/api/v1/admin/sla`, payload));
        this.slaMessage = 'New SLA rule added successfully.';
      }
      this.resetSlaForm();
      await this.loadSlaRules();
      this.drawerOpen = false;
    } catch (err) {
      this.slaError = 'Unable to save SLA rule. Please try again.';
    } finally {
      this.savingSla = false;
    }
  }
}
