import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthStore } from './auth.store';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);

  if (!auth.isAuthenticated() || auth.user()?.role !== 'ADMIN') {
    router.navigateByUrl('/dashboard');
    return false;
  }
  return true;
};
