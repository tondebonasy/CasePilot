import { Component, inject, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe, NgFor, NgIf } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { API_BASE } from '../../core/api';
import { DisputeFormComponent } from './dispute-form.component';

export interface Dispute {
  id: string;
  caseCode: string;
  merchantName: string;
  amount: number;
  currency: string;
  status: string;
  overdue: boolean;
  dueAt: string;
  network: string;
  resolutionOutcome?: string | null;
}

@Component({
  standalone: true,
  imports: [
    NgIf, NgFor, RouterLink, DatePipe, CurrencyPipe,
    MatTableModule, MatButtonModule, MatIconModule, MatDialogModule,
  ],
  templateUrl: './disputes.component.html',
  styleUrls: ['./disputes.component.scss'],
})
export class DisputesComponent implements OnInit {
  private http = inject(HttpClient);
  private dialog = inject(MatDialog);
  private router = inject(Router);

  dataSource = new MatTableDataSource<Dispute>();
  cols = ['case', 'merchant', 'amount', 'status', 'due', 'network'];
  loading = true;
  searchTerm = '';
  statusFilter = '';

  get statusOptions(): string[] {
    return [...new Set(this.dataSource.data.map(caseItem => caseItem.status))].sort();
  }

  get filteredDisputes(): Dispute[] {
    const search = this.searchTerm.trim().toLowerCase();
    return this.dataSource.data.filter(d =>
      (!this.statusFilter || d.status === this.statusFilter) &&
      (!search || [d.caseCode, d.merchantName, d.network, d.status].some(value =>
        String(value ?? '').toLowerCase().includes(search)
      ))
    );
  }

  exportCsv(): void {
    const rows = this.filteredDisputes;
    if (!rows.length) return;
    const fields: (keyof Dispute)[] = ['caseCode', 'merchantName', 'amount', 'currency', 'status', 'overdue', 'dueAt', 'network'];
    const escapeCsv = (input: unknown): string => {
      const cell = String(input ?? '');
      const safe = /^[\s]*[=+@\-]/.test(cell) ? "'" + cell : cell;
      return '"' + safe.replace(/"/g, '""') + '"';
    };
    const csv = [fields.join(','), ...rows.map(row => fields.map(field => escapeCsv(row[field])).join(','))].join('\r\n');
    const url = URL.createObjectURL(new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8' }));
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'casepilot-disputes.csv';
    anchor.click();
    URL.revokeObjectURL(url);
  }

  ngOnInit() {
    this.reload();
  }

  reload() {
    this.loading = true;
    this.http.get<any>(`${API_BASE}/api/v1/disputes`).subscribe({
      next: (res) => {
        this.dataSource.data = res.items ?? [];
        this.loading = false;
      },
      error: () => {
        this.dataSource.data = [];
        this.loading = false;
      }
    });
  }

  openCreateDisputeDialog(): void {
    const dialogRef = this.dialog.open(DisputeFormComponent, {
      width: '960px',
      maxWidth: '96vw',
      maxHeight: '94vh',
      autoFocus: 'first-tabbable',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.reload();
      }
    });
  }

  navigateToDispute(disputeId: string): void {
    this.router.navigate(['/disputes', disputeId]);
  }

  label(value: string): string {
    if (!value) return '';
    const cleaned = value.replace(/_/g, ' ').toLowerCase();
    return cleaned.replace(/\b\w/g, (c) => c.toUpperCase());
  }

  displayStatus(dispute: Dispute): string {
    if (dispute.status === 'CLOSED' && dispute.resolutionOutcome) {
      return `Closed - ${this.label(dispute.resolutionOutcome)}`;
    }
    return this.label(dispute.status);
  }

  statusIcon(status: string): string {
    switch (status) {
      case 'OPEN':
        return 'folder_open';
      case 'UNDER_REVIEW':
        return 'search';
      case 'NEEDS_INFO':
        return 'help';
      case 'READY_FOR_SUBMISSION':
        return 'send';
      case 'SUBMITTED':
        return 'outbox';
      case 'WON':
        return 'check_circle';
      case 'LOST':
        return 'cancel';
      case 'CLOSED':
        return 'task_alt';
      case 'CANCELLED':
        return 'block';
      case 'INELIGIBLE':
        return 'block';
      default:
        return 'flag';
    }
  }
}
