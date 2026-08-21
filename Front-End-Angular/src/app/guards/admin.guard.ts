import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Protege o painel.
 *
 * <p>Guard funcional, o formato atual do Angular — substitui a classe que
 * implementava CanActivate.
 *
 * <p>Isto e conveniencia de navegacao, nao seguranca: quem decide o que pode ser
 * lido ou escrito e o backend. Um token forjado no sessionStorage abre a tela e
 * nao traz dado nenhum.
 */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.token !== null ? true : router.createUrlTree(['/admin/login']);
};
