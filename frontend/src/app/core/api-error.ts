import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './models';

export function apiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as Partial<ApiError> | null;
    if (body?.message) {
      return body.message;
    }
    if (error.status === 0) {
      return 'Não foi possível conectar à API.';
    }
    if (error.status === 404) return 'Registro não encontrado.';
    if (error.status === 409) return 'Os dados informados já estão cadastrados.';
    if (error.status >= 500) return 'O servidor encontrou um erro inesperado.';
  }
  return 'Não foi possível concluir a operação.';
}

export function apiFieldErrors(error: unknown): Record<string, string> {
  if (error instanceof HttpErrorResponse) {
    return (error.error as Partial<ApiError> | null)?.fields ?? {};
  }
  return {};
}
