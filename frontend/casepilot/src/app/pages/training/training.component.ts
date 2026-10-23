import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, firstValueFrom, fromEvent, interval, merge, of } from 'rxjs';
import { DatePipe, DecimalPipe, NgFor, NgIf, TitleCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { API_BASE } from '../../core/api';
import { apiErrorMessage } from '../../core/api-error';
interface Policy { id: number; title: string; }
interface Quiz { id: number; title: string; description: string; policyId: number; }
interface AttemptSummary { id: number; quizTitle: string; score: number; maxScore: number; createdAt: string; }
interface Metrics {
  averageScore: number;
  completionCount: number;
  byPolicy: { policyId: number; policyTitle: string; averageScore: number; attempts: number }[];
  byRole: { role: string; averageScore: number; attempts: number }[];
}

@Component({
  standalone: true,
  imports: [
    NgIf, NgFor, DatePipe, DecimalPipe, RouterLink,
    TitleCasePipe,
    MatCardModule, MatButtonModule, MatTabsModule, MatIconModule, MatTableModule
  ],
  templateUrl: './training.component.html',
  styleUrls: ['./training.component.scss']
})
export class TrainingComponent implements OnInit {
  private http = inject(HttpClient);
  private destroyRef = inject(DestroyRef);

  policies: Policy[] = [];
  quizzes: Quiz[] = [];
  attempts: AttemptSummary[] = [];
  metrics: Metrics | null = null;
  loading = true;
  error = '';
  private refreshing = false;
  policyColumns = ['policy', 'average', 'attempts'];
  roleColumns = ['role', 'average', 'attempts'];

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
        void this.refresh();
      });
  }

  async refresh() {
    if (this.refreshing) return;
    this.refreshing = true;
    this.error = '';
    this.loading = true;
    try {
      [this.policies, this.quizzes, this.metrics, this.attempts] = await Promise.all([
        firstValueFrom(this.http.get<Policy[]>(`${API_BASE}/api/v1/training/policies`)),
        firstValueFrom(this.http.get<Quiz[]>(`${API_BASE}/api/v1/training/quizzes`)),
        firstValueFrom(this.http.get<Metrics>(`${API_BASE}/api/v1/training/metrics`)),
        firstValueFrom(this.http.get<AttemptSummary[]>(`${API_BASE}/api/v1/training/attempts/me`))
      ]);
    } catch (error) {
      this.error = apiErrorMessage(error, 'load training data');
    } finally {
      this.loading = false;
      this.refreshing = false;
    }
  }

  policyTitle(policyId: number): string {
    const policy = this.policies.find(p => p.id === policyId);
    return policy ? policy.title : 'Unknown Policy';
  }

  async exportCsv() {
    const blob = await firstValueFrom(
      this.http.get(`${API_BASE}/api/v1/training/metrics/export`, { responseType: 'blob' })
    );
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `training-metrics-${this.localDateStamp()}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  private localDateStamp(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = `${now.getMonth() + 1}`.padStart(2, '0');
    const day = `${now.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
