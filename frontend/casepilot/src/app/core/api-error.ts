import { HttpErrorResponse } from '@angular/common/http';

export function apiErrorMessage(error: unknown, action: string): string {
  if (!(error instanceof HttpErrorResponse)) return `Unable to ${action}. Please try again.`;
  if (error.status === 401) return 'Your session has expired. Please sign in again.';
  if (error.status === 403) return `You do not have permission to ${action}.`;
  if (error.status === 0) return 'Cannot reach CasePilot. Check your connection and try again.';
  if ([502, 503, 504].includes(error.status)) return 'The service is temporarily unavailable. Please try again.';
  if (error.status === 404) return 'This service could not be found. Please refresh the page or contact your administrator.';
  return `Unable to ${action}. Please try again. If the problem continues, contact your administrator.`;
}
