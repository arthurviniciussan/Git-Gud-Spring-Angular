import { HttpClient } from '@angular/common/http';
import { computed, Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { RespostaDeLogin, Sessao } from '../types/auth.type';

/**
 * Sessao do admin.
 *
 * <p>Guarda o token em sessionStorage: some ao fechar a aba, o que e o
 * comportamento desejado para um painel de administracao. Quem envia o token nas
 * requisicoes e o authInterceptor — antes nada enviava, e o token ficava
 * guardado sem nunca ser usado.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private static readonly CHAVE_TOKEN = 'gitgud.token';

  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/auth`;

  private readonly sessaoAtual = signal<Sessao | null>(null);

  readonly sessao = this.sessaoAtual.asReadonly();
  readonly autenticado = computed(() => this.sessaoAtual() !== null);

  login(email: string, password: string): Observable<RespostaDeLogin> {
    return this.http
      .post<RespostaDeLogin>(`${this.url}/login`, { email, password })
      .pipe(
        tap((resposta) => {
          this.guardarToken(resposta.token);
          this.sessaoAtual.set({
            name: resposta.name,
            email: resposta.email,
            role: resposta.role,
          });
        }),
      );
  }

  /** Confere no servidor se o token guardado ainda vale. */
  carregarSessao(): Observable<Sessao> {
    return this.http
      .get<Sessao>(`${this.url}/session`)
      .pipe(tap((sessao) => this.sessaoAtual.set(sessao)));
  }

  logout(): void {
    this.guardarToken(null);
    this.sessaoAtual.set(null);
  }

  get token(): string | null {
    // Sem armazenamento disponivel (renderizacao no servidor, aba anonima com
    // storage bloqueado) simplesmente nao ha sessao.
    if (typeof sessionStorage === 'undefined') {
      return null;
    }
    return sessionStorage.getItem(AuthService.CHAVE_TOKEN);
  }

  private guardarToken(token: string | null): void {
    if (typeof sessionStorage === 'undefined') {
      return;
    }
    if (token === null) {
      sessionStorage.removeItem(AuthService.CHAVE_TOKEN);
    } else {
      sessionStorage.setItem(AuthService.CHAVE_TOKEN, token);
    }
  }
}
