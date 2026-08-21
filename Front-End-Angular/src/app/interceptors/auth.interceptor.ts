import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Anexa o token e reage a sessao expirada.
 *
 * <p>Esta peca nao existia: o token era gravado no sessionStorage e nunca
 * enviado, entao nenhuma rota autenticada funcionava de fato.
 */
export const authInterceptor: HttpInterceptorFn = (requisicao, proxima) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const token = auth.token;
  const comToken = token
    ? requisicao.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : requisicao;

  return proxima(comToken).pipe(
    catchError((erro: HttpErrorResponse) => {
      // O 401 do proprio login e "senha errada", nao "sessao expirada":
      // redirecionar aqui engoliria a mensagem de erro na tela de login.
      const ehTentativaDeLogin = requisicao.url.endsWith('/auth/login');

      if (erro.status === 401 && !ehTentativaDeLogin) {
        auth.logout();
        void router.navigate(['/admin/login']);
      }

      return throwError(() => erro);
    }),
  );
};
