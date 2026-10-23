import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStore } from './auth.store';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthStore);
  const token = auth.token();

  const isPublic = req.url.includes('/api/v1/auth/login') || req.url.includes('/api/v1/auth/register');
  if (token && !isPublic) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` }});
  }
  return next(req);
};
