import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { CurrencyPipe, DatePipe, NgFor, NgIf, TitleCasePipe } from '@angular/common';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { API_BASE } from '../../core/api';
import { AuthStore } from '../../core/auth.store';

type ActionConfig = { label: string; status: string; detail: string; color?: 'primary' | 'warn' };

@Component({
  standalone: true,
  imports: [
    NgIf, NgFor, RouterLink, DatePipe, CurrencyPipe, TitleCasePipe,
    MatCardModule, MatButtonModule, MatIconModule, MatDividerModule,
  ],
  templateUrl: './dispute-detail.component.html',
  styleUrls: ['./dispute-detail.component.scss'],
})
export class DisputeDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);
  private auth = inject(AuthStore);
  private snackBar = inject(MatSnackBar);

  d: any;
  loading = true;
  uploading = false;
  claiming = false;
  reopening = false;
  error = '';

  ngOnInit() {
    this.reload();
  }

  reload() {
    this.loading = true;
    this.error = '';
    const id = this.route.snapshot.paramMap.get('id');
    this.http.get(`${API_BASE}/api/v1/disputes/${id}`).subscribe({
      next: (res) => {
        this.d = res;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load this dispute.';
        this.loading = false;
      }
    });
  }

  updateStatus(newStatus: string, detail: string) {
    if (!this.d) return;
    const id = this.d.id;
    this.http.put(`${API_BASE}/api/v1/disputes/${id}/status`, { status: newStatus, detail })
      .subscribe({
        next: () => this.reload(),
        error: () => { this.error = 'Unable to update status.'; }
      });
  }

  claimCase() {
    if (!this.d) return;
    this.claiming = true;
    const id = this.d.id;
    this.http.put(`${API_BASE}/api/v1/disputes/${id}/claim`, {})
      .subscribe({
        next: () => {
          this.claiming = false;
          this.reload();
        },
        error: () => {
          this.claiming = false;
          this.error = 'Unable to claim this case.';
        }
      });
  }

  onFile(ev: any) {
    const file = ev.target.files?.[0];
    if (!file || !this.d) return;

    const fd = new FormData();
    fd.append('file', file);

    this.uploading = true;
    this.http.post(`${API_BASE}/api/v1/disputes/${this.d.id}/evidence`, fd)
      .subscribe({
        next: () => {
          this.uploading = false;
          ev.target.value = '';
          this.snackBar.open('Evidence uploaded. The case status has not changed. Move it to the next stage when ready.', 'Dismiss', { duration: 5000 });
          this.reload();
        },
        error: () => {
          this.uploading = false;
          ev.target.value = '';
          this.error = 'Unable to upload evidence.';
        }
      });
  }

  reopenCase() {
    if (!this.d || !this.isAdmin()) return;
    this.reopening = true;
    this.http.put(`${API_BASE}/api/v1/disputes/${this.d.id}/reopen`, {})
      .subscribe({
        next: () => {
          this.reopening = false;
          this.snackBar.open('Case reopened.', 'Dismiss', { duration: 4000 });
          this.reload();
        },
        error: () => {
          this.reopening = false;
          this.error = 'Unable to reopen this case.';
        }
      });
  }

  currentRole(): string {
    let role = (this.auth.user()?.role ?? '').toUpperCase();
    if (role.startsWith('ROLE_')) {
      role = role.slice(5);
    }
    return role;
  }

  currentUserId(): number {
    return this.auth.user()?.id ?? 0;
  }

  isAdmin(): boolean {
    return this.currentRole() === 'ADMIN';
  }

  roleForStatus(status: string): string {
    const map: Record<string, string> = {
      OPEN: 'INTAKE_OFFICER',
      UNDER_REVIEW: 'REVIEWER',
      NEEDS_INFO: 'EVIDENCE_OFFICER',
      READY_FOR_SUBMISSION: 'NETWORK_LIAISON',
      SUBMITTED: 'RESOLUTION_OFFICER',
      WON: 'RESOLUTION_OFFICER',
      LOST: 'RESOLUTION_OFFICER',
      CLOSED: 'RESOLUTION_OFFICER'
    };
    return map[status] ?? 'N/A';
  }

  formatLabel(value: string): string {
    if (!value) return '';
    const cleaned = value.replace(/_/g, ' ').toLowerCase();
    return cleaned.replace(/\b\w/g, (c) => c.toUpperCase());
  }

  statusIcon(status: string): string {
    switch (status) {
      case 'INELIGIBLE':
        return 'block';
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
      default:
        return 'flag';
    }
  }

  canClaim(): boolean {
    if (!this.d) return false;
    if (this.isAdmin()) {
      return this.d.assignedToUserId == null && !['INELIGIBLE', 'CANCELLED', 'CLOSED'].includes(this.d.status);
    }
    const role = this.currentRole();
    return this.d.assignedToUserId == null && role !== '' && role === this.roleForStatus(this.d.status);
  }

  displayStatusLabel(): string {
    if (!this.d) return '';
    if (this.d.status === 'CLOSED' && this.d.resolutionOutcome) {
      return `Closed - ${this.formatLabel(this.d.resolutionOutcome)}`;
    }
    return this.formatLabel(this.d.status);
  }

  settlementLabel(): string {
    if (!this.d?.resolutionOutcome) {
      return this.d?.status === 'INELIGIBLE' ? 'Not settled' : 'Pending';
    }
    return this.formatLabel(this.d.resolutionOutcome);
  }

  isAssignedToMe(): boolean {
    if (!this.d) return false;
    if (this.isAdmin()) return true;
    return this.d.assignedToUserId === this.currentUserId();
  }

  canAct(): boolean {
    if (!this.d) return false;
    if (this.isAdmin()) return true;
    const role = this.currentRole();
    return this.isAssignedToMe() && role === this.roleForStatus(this.d.status);
  }

  canUploadEvidence(): boolean {
    if (!this.d) return false;
    const evidenceStages = ['NEEDS_INFO', 'READY_FOR_SUBMISSION'];
    const role = this.currentRole();
    if (this.isAdmin()) return evidenceStages.includes(this.d.status);
    if (role === 'CUSTOMER') {
      return this.d.openedByUserId === this.currentUserId() && evidenceStages.includes(this.d.status);
    }
    return this.isAssignedToMe() && evidenceStages.includes(this.d.status);
  }

  assignmentLabel(): string {
    if (!this.d) return '';
    if (this.isAdmin()) {
      return this.d.assignedToUserId == null ? 'Admin override available' : `Admin override on User #${this.d.assignedToUserId}`;
    }
    if (this.d.assignedToUserId == null) {
      return 'Unassigned';
    }
    if (this.d.assignedToUserId === this.currentUserId()) {
      return 'You';
    }
    return `User #${this.d.assignedToUserId}`;
  }

  statusActions(): ActionConfig[] {
    if (!this.d) return [];
    const status = this.d.status as string;
    switch (status) {
      case 'OPEN':
        return [{ label: 'Send to Reviewer', status: 'UNDER_REVIEW', detail: 'Assigned to reviewer.' }];
      case 'UNDER_REVIEW':
        return [{ label: 'Request Evidence', status: 'NEEDS_INFO', detail: 'Evidence requested.' }];
      case 'NEEDS_INFO':
        return [{ label: 'Evidence Collected', status: 'READY_FOR_SUBMISSION', detail: 'Evidence collected.' }];
      case 'READY_FOR_SUBMISSION':
        return [{ label: 'Submit to Network', status: 'SUBMITTED', detail: 'Submitted to network.' }];
      case 'SUBMITTED':
        return [
          { label: 'Mark Won', status: 'WON', detail: 'Network decision: won.', color: 'primary' },
          { label: 'Mark Lost', status: 'LOST', detail: 'Network decision: lost.', color: 'warn' }
        ];
      case 'WON':
        return [{ label: 'Close Case', status: 'CLOSED', detail: 'Case closed after win.' }];
      case 'LOST':
        return [{ label: 'Close Case', status: 'CLOSED', detail: 'Case closed after loss.' }];
      default:
        return [];
    }
  }

  nextStageLabel(): string {
    if (!this.d) return 'N/A';
    const status = this.d.status as string;
    const next = this.statusActions()[0];
    if (status === 'INELIGIBLE') {
      return 'This case cannot continue';
    }
    if (status === 'CLOSED' || status === 'CANCELLED') {
      return 'Case completed';
    }
    return next ? this.formatLabel(next.status) : 'No next stage';
  }

  nextStageRoleLabel(): string {
    const next = this.statusActions()[0];
    return next ? this.formatLabel(this.roleForStatus(next.status)) : 'N/A';
  }

  actionHint(): string {
    if (!this.d) return '';
    if (this.d.status === 'INELIGIBLE') {
      return 'This case is ineligible. It failed the eligibility checks and will not move into the dispute workflow.';
    }
    if (this.d.status === 'CLOSED' && this.d.resolutionOutcome) {
      return `This case is closed with outcome ${this.formatLabel(this.d.resolutionOutcome)}. Only an admin can reopen it.`;
    }
    if (this.isAdmin()) {
      return `Admin override is enabled. You can perform the current role's task, but the workflow order is still enforced. The next required stage is ${this.nextStageLabel()}.`;
    }
    if (!this.statusActions().length) {
      return 'No further actions are available for this status.';
    }
    if (this.canClaim()) {
      return 'Claim this case first, then move it to the next required stage.';
    }
    if (!this.canAct()) {
      return `Execution order is enforced. The next acting role is ${this.nextStageRoleLabel()}.`;
    }
    return `The next required stage is ${this.nextStageLabel()}. Do not skip any workflow step.`;
  }

  viewEvidence(evidenceId: number, fileName: string) {
    this.fetchEvidenceBlob(evidenceId, 'view').subscribe({
      next: (response) => {
        const blob = response.body;
        if (!blob) {
          throw new Error('Empty response body');
        }
        const objectUrl = URL.createObjectURL(blob);
        const popup = window.open(objectUrl, '_blank', 'noopener');
        if (!popup) {
          this.saveBlob(blob, this.resolveDownloadFileName(response, fileName));
        }
        window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
      },
      error: () => {
        this.snackBar.open(`Unable to open ${fileName}.`, 'Dismiss', { duration: 4000 });
      }
    });
  }

  downloadEvidence(evidenceId: number, fileName: string) {
    this.fetchEvidenceBlob(evidenceId, 'download').subscribe({
      next: (response) => {
        const blob = response.body;
        if (!blob) {
          throw new Error('Empty response body');
        }
        this.saveBlob(blob, this.resolveDownloadFileName(response, fileName));
      },
      error: () => {
        this.snackBar.open(`Unable to download ${fileName}.`, 'Dismiss', { duration: 4000 });
      }
    });
  }

  private fetchEvidenceBlob(evidenceId: number, mode: 'view' | 'download') {
    return this.http.get(
      `${API_BASE}/api/v1/disputes/${this.d.id}/evidence/${evidenceId}/${mode}`,
      { responseType: 'blob', observe: 'response' }
    );
  }

  private saveBlob(blob: Blob, fileName: string) {
    const nav = window.navigator as Navigator & { msSaveOrOpenBlob?: (blob: Blob, defaultName?: string) => boolean };
    if (nav.msSaveOrOpenBlob) {
      nav.msSaveOrOpenBlob(blob, fileName);
      return;
    }

    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = fileName;
    link.rel = 'noopener';
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 10_000);
  }

  private resolveDownloadFileName(response: HttpResponse<Blob>, fallback: string): string {
    const contentDisposition = response.headers.get('content-disposition') ?? '';
    const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match?.[1]) {
      return decodeURIComponent(utf8Match[1]);
    }

    const plainMatch = contentDisposition.match(/filename="([^"]+)"/i) ?? contentDisposition.match(/filename=([^;]+)/i);
    const extracted = plainMatch?.[1]?.trim();
    return extracted && extracted.length > 0 ? extracted : fallback;
  }
}
