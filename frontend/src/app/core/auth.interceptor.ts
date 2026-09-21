import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Anexa o token em toda chamada e trata o 401 num lugar só: sessão inválida
 * derruba o login e leva de volta para a tela de entrada, guardando a rota
 * pedida para voltar a ela depois.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const token = auth.token();
  const requisicao = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(requisicao).pipe(
    catchError((erro: unknown) => {
      if (erro instanceof HttpErrorResponse && erro.status === 401 && auth.autenticado()) {
        auth.sair(router.url);
      }
      return throwError(() => erro);
    }),
  );
};
