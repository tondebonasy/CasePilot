import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';
import { AuthStore } from './auth.store';

let lastAuthToastAt = 0;
const AUTH_TOAST_THROTTLE_MS = 2000;

const notifyOnce = (snackBar: MatSnackBar, message: string) => {
  const now = Date.now();
  if (now - lastAuthToastAt < AUTH_TOAST_THROTTLE_MS) {
    return;
  }
  lastAuthToastAt = now;
  snackBar.open(message, 'Dismiss', { duration: 4000 });
};

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const auth = inject(AuthStore);
  const snackBar = inject(MatSnackBar);

  const isAuthRequest = req.url.includes('/api/v1/auth/');

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !isAuthRequest) {
        auth.logout();
        if (router.url !== '/login') {
          notifyOnce(snackBar, 'Session expired. Please log in again.');
          router.navigateByUrl('/login');
        }
      } else if (error.status === 403) {
        if (!auth.isAuthenticated()) {
          auth.logout();
          if (router.url !== '/login') {
            notifyOnce(snackBar, 'Please log in to continue.');
            router.navigateByUrl('/login');
          }
        } else {
          const adminOnly = router.url.startsWith('/admin') || req.url.includes('/api/v1/admin/');
          const message = adminOnly
            ? 'Admin access required for that action.'
            : 'You do not have permission to perform this action.';
          notifyOnce(snackBar, message);
          if (adminOnly && router.url.startsWith('/admin')) {
            router.navigateByUrl('/dashboard');
          }
        }
      }

      return throwError(() => error);
    })
  );
};
