import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Rotas que só fazem sentido logado. Quem cair aqui deslogado vai para o
 * login com `redirect`, e volta para onde queria assim que entrar.
 */
export const authGuard: CanActivateFn = (_rota, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.autenticado()) {
    return true;
  }

  return router.createUrlTree(['/login'], { queryParams: { redirect: estado.url } });
};
