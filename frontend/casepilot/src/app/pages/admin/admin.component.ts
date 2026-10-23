import { Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { SlaManagerComponent } from './sla-manager.component';
import { PolicyManagerComponent } from './policy-manager.component';
import { QuizManagerComponent } from './quiz-manager.component';
import { AdminAuditLogComponent } from './admin-audit-log.component';
import { AdminUsersComponent } from './admin-users.component';

@Component({
  standalone: true,
  imports: [
    MatTabsModule,
    MatIconModule,
    SlaManagerComponent,
    PolicyManagerComponent,
    QuizManagerComponent,
    AdminAuditLogComponent,
    AdminUsersComponent,
  ],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss'],
})
export class AdminComponent {
}
