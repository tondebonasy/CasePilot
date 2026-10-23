import { apiErrorMessage } from '../../core/api-error';
import { Component, inject, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { NgFor, NgIf } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { API_BASE } from '../../core/api';
import { Policy } from './policy-manager.component';

export interface QuizOption {
  id: number;
  text: string;
  correct: boolean;
}

export interface QuizQuestion {
  id: number;
  question: string;
  explanation: string | null;
  options: QuizOption[];
}

export interface Quiz {
  id: number;
  title: string;
  description: string;
  policyId: number;
  questions: QuizQuestion[];
}

@Component({
  selector: 'app-quiz-manager',
  standalone: true,
  imports: [
    ReactiveFormsModule, NgFor, NgIf,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatCheckboxModule, MatIconModule, MatSidenavModule
  ],
  templateUrl: './quiz-manager.component.html',
  styleUrls: ['./quiz-manager.component.scss']
})
export class QuizManagerComponent implements OnInit {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);

  policies: Policy[] = [];
  quizzes: Quiz[] = [];
  loading = true;
  loadFailed = false;
  policyWarning = '';
  drawerOpen = false;

  quizForm = this.fb.group({
    policyId: [null as number | null, Validators.required],
    title: ['', Validators.required],
    description: ['', Validators.required],
    questions: this.fb.array([this.createQuestionGroup()])
  });

  submittingQuiz = false;
  quizMessage = '';
  quizError = '';

  ngOnInit() {
    this.loadData();
  }

  async loadData() {
    this.loadFailed = false;
    this.quizError = '';
    this.loading = true;
    try {
      [this.policies, this.quizzes] = await Promise.all([
        firstValueFrom(this.http.get<Policy[]>(`${API_BASE}/api/v1/admin/training/policies`)),
        firstValueFrom(this.http.get<Quiz[]>(`${API_BASE}/api/v1/admin/training/quizzes`))
      ]);
      this.policies = this.policies.filter(p => p.active);
      this.policyWarning = '';
      const policyControl = this.quizForm.get('policyId');
      if (this.policies.length > 0) {
        policyControl?.enable({ emitEvent: false });
        policyControl?.setValue(this.policies[0].id, { emitEvent: false });
      } else {
        policyControl?.setValue(null, { emitEvent: false });
        policyControl?.disable({ emitEvent: false });
        this.policyWarning = 'No active policies available. Create or activate a policy first.';
      }
    } catch (error) {
      this.loadFailed = true;
      this.quizError = apiErrorMessage(error, 'load policies and quizzes');
    } finally {
      this.loading = false;
    }
  }

  get questionControls() {
    return this.quizForm.get('questions') as FormArray;
  }

  questionOptions(index: number) {
    return this.questionControls.at(index).get('options') as FormArray;
  }

  private createQuestionGroup() {
    return this.fb.group({
      question: ['', Validators.required],
      explanation: [''],
      options: this.fb.array([this.createOptionGroup(true), this.createOptionGroup(false)])
    });
  }

  private createOptionGroup(correct = false) {
    return this.fb.group({
      text: ['', Validators.required],
      correct: [correct]
    });
  }

  addQuestion() {
    this.questionControls.push(this.createQuestionGroup());
  }

  removeQuestion(index: number) {
    if (this.questionControls.length > 1) {
      this.questionControls.removeAt(index);
    }
  }

  addOption(questionIndex: number) {
    this.questionOptions(questionIndex).push(this.createOptionGroup());
  }

  removeOption(questionIndex: number, optionIndex: number) {
    if (this.questionOptions(questionIndex).length > 2) {
      this.questionOptions(questionIndex).removeAt(optionIndex);
    }
  }

  async submitQuiz() {
    if (this.quizForm.invalid) {
      this.quizForm.markAllAsTouched();
      return;
    }
    this.submittingQuiz = true;
    this.quizError = '';
    this.quizMessage = '';

    try {
      await firstValueFrom(this.http.post(`${API_BASE}/api/v1/admin/training/quizzes`, this.quizForm.value));
      this.quizMessage = 'Quiz created successfully.';
      this.resetQuizForm();
      await this.loadData();
      this.drawerOpen = false;
    } catch (err) {
      this.quizError = 'Unable to create quiz.';
    } finally {
      this.submittingQuiz = false;
    }
  }

  private resetQuizForm() {
    this.quizForm.reset();
    this.questionControls.clear();
    this.questionControls.push(this.createQuestionGroup());
    const policyControl = this.quizForm.get('policyId');
    if (this.policies.length > 0) {
      policyControl?.enable({ emitEvent: false });
      policyControl?.setValue(this.policies[0].id, { emitEvent: false });
    } else {
      policyControl?.setValue(null, { emitEvent: false });
      policyControl?.disable({ emitEvent: false });
    }
  }

  openCreateDrawer() {
    this.quizMessage = '';
    this.quizError = '';
    this.resetQuizForm();
    this.drawerOpen = true;
  }

  closeDrawer() {
    this.drawerOpen = false;
    this.resetQuizForm();
  }

  policyTitle(policyId: number) {
    const policy = this.policies.find(p => p.id === policyId);
    return policy ? policy.title : 'Unknown Policy';
  }
}
