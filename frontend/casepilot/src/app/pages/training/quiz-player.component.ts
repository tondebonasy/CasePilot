import { Component, inject, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { DecimalPipe, NgFor, NgIf } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { API_BASE } from '../../core/api';
interface QuizOption { id: number; text: string; }
interface QuizQuestion { id: number; question: string; options: QuizOption[]; }
interface Quiz { id: number; title: string; description: string; policyId: number; questions: QuizQuestion[]; }
interface QuizSubmission {
  score: number;
  maxScore: number;
  weaknesses: { questionId: number; question: string; explanation: string }[];
}

@Component({
  selector: 'app-quiz-player',
  standalone: true,
  imports: [
    NgIf, NgFor, DecimalPipe, RouterLink,
    MatCardModule, MatButtonModule, MatRadioModule, MatIconModule, MatProgressBarModule
  ],
  templateUrl: './quiz-player.component.html',
  styleUrls: ['./quiz-player.component.scss']
})
export class QuizPlayerComponent implements OnInit {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);

  quiz: Quiz | null = null;
  loading = true;
  error = '';

  currentQuestionIndex = 0;
  answers: Record<number, number> = {};

  submitting = false;
  result: QuizSubmission | null = null;

  ngOnInit() {
    this.loadQuiz();
  }

  async loadQuiz() {
    this.loading = true;
    const id = this.route.snapshot.paramMap.get('id');
    try {
      this.quiz = await firstValueFrom(this.http.get<Quiz>(`${API_BASE}/api/v1/training/quizzes/${id}`));
    } catch (e) {
      this.error = 'Could not load the selected quiz.';
    } finally {
      this.loading = false;
    }
  }

  selectAnswer(questionId: number, optionId: number) {
    this.answers[questionId] = optionId;
  }

  nextQuestion() {
    if (!this.quiz || this.currentQuestionIndex >= this.quiz.questions.length - 1) return;
    this.currentQuestionIndex++;
  }

  prevQuestion() {
    if (this.currentQuestionIndex > 0) {
      this.currentQuestionIndex--;
    }
  }

  get progress() {
    if (!this.quiz) return 0;
    return ((this.currentQuestionIndex + 1) / this.quiz.questions.length) * 100;
  }

  isAnswered(questionId: number): boolean {
    return this.answers.hasOwnProperty(questionId);
  }

  allQuestionsAnswered(): boolean {
    if (!this.quiz) return false;
    return this.quiz.questions.every(q => this.isAnswered(q.id));
  }

  async submitQuiz() {
    if (!this.quiz || !this.allQuestionsAnswered()) return;

    this.submitting = true;
    const payload = {
      quizId: this.quiz.id,
      answers: Object.entries(this.answers).map(([questionId, optionId]) => ({
        questionId: +questionId,
        optionId
      }))
    };

    try {
      this.result = await firstValueFrom(this.http.post<QuizSubmission>(`${API_BASE}/api/v1/training/attempts`, payload));
    } catch (e) {
      this.error = 'There was an error submitting your answers. Please try again.';
    } finally {
      this.submitting = false;
    }
  }

  get resultPercentage(): number {
    if (!this.result || this.result.maxScore === 0) return 0;
    return (this.result.score / this.result.maxScore) * 100;
  }
}
