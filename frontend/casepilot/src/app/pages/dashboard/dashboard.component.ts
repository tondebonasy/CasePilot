import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DatePipe, DecimalPipe, NgFor, NgIf } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, firstValueFrom, fromEvent, interval, merge, of } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { API_BASE } from '../../core/api';
import { apiErrorMessage } from '../../core/api-error';
import { AuthStore } from '../../core/auth.store';

interface DisputeSummary {
  id: number;
  caseCode: string;
  merchantName: string;
  status: string;
  dueAt: string;
  overdue: boolean;
}

@Component({
  standalone: true,
  imports: [
    NgIf,
    NgFor,
    DecimalPipe,
    DatePipe,
    RouterLink,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {
  private http = inject(HttpClient);
  private auth = inject(AuthStore);
  private destroyRef = inject(DestroyRef);

  m: any;
  loading = true;
  metricsError = '';
  tasks: DisputeSummary[] = [];
  queue: DisputeSummary[] = [];
  queueLoading = true;
  queueError = '';

  ngOnInit() {
    merge(
      of(null),
      interval(30000),
      fromEvent(window, 'focus'),
      fromEvent(document, 'visibilitychange').pipe(
        filter(() => document.visibilityState === 'visible')
      )
    )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        void this.loadMetrics();
        void this.loadWorkQueues();
      });
  }

  showWorkQueue(): boolean {
    let role = (this.auth.user()?.role ?? '').toUpperCase();
    if (role.startsWith('ROLE_')) {
      role = role.slice(5);
    }
    return role !== '' && role !== 'CUSTOMER';
  }

  async loadWorkQueues() {
    if (!this.showWorkQueue()) {
      return;
    }
    this.queueLoading = true;
    this.queueError = '';
    try {
      const [tasks, queue] = await Promise.all([
        firstValueFrom(this.http.get<DisputeSummary[]>(`${API_BASE}/api/v1/disputes/tasks`)),
        firstValueFrom(this.http.get<DisputeSummary[]>(`${API_BASE}/api/v1/disputes/queue`))
      ]);
      this.tasks = tasks ?? [];
      this.queue = queue ?? [];
    } catch (e) {
      this.queueError = 'Unable to load your work queue.';
    } finally {
      this.queueLoading = false;
    }
  }

  async exportCsv() {
    const blob = await firstValueFrom(
      this.http.get(`${API_BASE}/api/v1/metrics/dashboard/export`, { responseType: 'blob' })
    );
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `dashboard-metrics-${this.localDateStamp()}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  async exportBankCsv() {
    const blob = await firstValueFrom(
      this.http.get(`${API_BASE}/api/v1/metrics/bank/export`, { responseType: 'blob' })
    );
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `bank-report-${this.localDateStamp()}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  async claimCase(id: number) {
    try {
      await firstValueFrom(this.http.put(`${API_BASE}/api/v1/disputes/${id}/claim`, {}));
      await this.loadWorkQueues();
    } catch (e) {
      this.queueError = 'Unable to claim this case.';
    }
  }

  label(value: string): string {
    if (!value) return '';
    const cleaned = value.replace(/_/g, ' ').toLowerCase();
    return cleaned.replace(/\b\w/g, (c) => c.toUpperCase());
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

  async loadMetrics() {
    this.metricsError = '';
    try {
      this.m = await firstValueFrom(this.http.get(`${API_BASE}/api/v1/metrics/dashboard`));
    } catch (error) {
      this.metricsError = apiErrorMessage(error, 'load dashboard metrics');
    } finally {
      this.loading = false;
    }
  }

  private localDateStamp(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = `${now.getMonth() + 1}`.padStart(2, '0');
    const day = `${now.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
