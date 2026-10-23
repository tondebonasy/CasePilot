import { apiErrorMessage } from '../../core/api-error';
import { Component, inject, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DatePipe, NgFor, NgIf, TitleCasePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { API_BASE } from '../../core/api';

interface AuditLog {
  id: number;
  actorUserId: number;
  actorRole: string;
  action: string;
  detail: string;
  ipAddress: string;
  atTime: string;
}

@Component({
  selector: 'app-admin-audit-log',
  standalone: true,
  imports: [
    NgIf, NgFor, DatePipe, TitleCasePipe,
    MatCardModule, MatTableModule, MatIconModule, MatButtonModule,
  ],
  templateUrl: './admin-audit-log.component.html',
  styleUrls: ['./admin-audit-log.component.scss']
})
export class AdminAuditLogComponent implements OnInit {
  private http = inject(HttpClient);

  logs: AuditLog[] = [];
  loading = true;
  loadFailed = false;
  error = '';
  columns = ['time', 'actor', 'action', 'detail', 'ip'];

  ngOnInit() {
    this.load();
  }

  async load() {
    this.loadFailed = false;
    this.error = '';
    this.loading = true;
    this.error = '';
    try {
      this.logs = await firstValueFrom(this.http.get<AuditLog[]>(`${API_BASE}/api/v1/admin/audit`));
    } catch (e) {
      this.loadFailed = true;
      this.error = apiErrorMessage(e, 'load audit logs');
    } finally {
      this.loading = false;
    }
  }
}
