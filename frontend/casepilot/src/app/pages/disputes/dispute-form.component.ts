import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { NgFor, NgIf } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { API_BASE } from '../../core/api';

@Component({
  selector: 'app-dispute-form',
  standalone: true,
  imports: [
    ReactiveFormsModule, NgIf, NgFor,
    MatDialogModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatIconModule, MatDividerModule, MatCheckboxModule,
    MatDatepickerModule, MatNativeDateModule,
  ],
  templateUrl: './dispute-form.component.html',
  styleUrls: ['./dispute-form.component.scss'],
})
export class DisputeFormComponent {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private dialogRef = inject(MatDialogRef<DisputeFormComponent>);

  creating = false;
  networks = ['VISA', 'MASTERCARD', 'ZIMSWITCH'];
  channels = ['POS', 'E-COMMERCE', 'MOBILE', 'BRANCH', 'ATM'];
  disputeTypes = [
    'UNAUTHORIZED', 'DUPLICATE', 'NOT_RECEIVED',
    'DEFECTIVE_OR_NOT_AS_DESCRIBED', 'CANCELLED_RECURRING',
    'PROCESSING_ERROR', 'OTHER'
  ];

  form = this.fb.group({
    merchantName: ['', Validators.required],
    merchantId: ['', Validators.required],
    cardLast4: ['', [Validators.required, Validators.pattern(/^\d{4}$/)]],
    amount: ['', [Validators.required, Validators.min(0.01)]],
    currency: ['USD', Validators.required],
    txDate: [new Date(), Validators.required],
    network: ['VISA', Validators.required],
    transactionRef: ['', Validators.required],
    rrn: ['', Validators.required],
    authCode: ['', Validators.required],
    channel: ['POS', Validators.required],
    terminalId: ['', Validators.required],
    terminalLocation: ['', Validators.required],
    customerIdentifier: ['', Validators.required],
    merchantRefunded: [false],
    refundRef: [''],
    refundAt: [null as Date | null],
    disputeType: ['UNAUTHORIZED', Validators.required],
    reason: ['', Validators.required]
  });

  async create() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.creating = true;
    const raw = this.form.value;
    const amountValue = Number(raw.amount ?? 0);
    const txValue = this.toIsoDate(raw.txDate);
    const refundValue = raw.refundAt ? this.toIsoDate(raw.refundAt) : null;
    const payload = { ...raw, amount: amountValue, txDate: txValue, refundAt: refundValue };

    try {
      const result = await firstValueFrom(this.http.post(`${API_BASE}/api/v1/disputes`, payload));
      this.dialogRef.close(result);
    } catch (error) {
      console.error('Failed to create dispute', error);
    } finally {
      this.creating = false;
    }
  }

  close() {
    this.dialogRef.close();
  }

  private toIsoDate(value: string | Date | null | undefined): string {
    if (!value) {
      return new Date().toISOString();
    }

    if (value instanceof Date) {
      return new Date(value.getFullYear(), value.getMonth(), value.getDate()).toISOString();
    }

    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return new Date(`${value}T00:00:00`).toISOString();
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return new Date().toISOString();
    }
    return date.toISOString();
  }
}
