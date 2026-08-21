import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

/**
 * Painel do admin.
 *
 * <p>Placeholder ate a Etapa 4, quando recebe a lista de artigos e o editor.
 * Serve, por enquanto, para provar que o token chega ao backend: a tela so
 * aparece preenchida se GET /api/auth/session responder 200.
 */
@Component({
  selector: 'app-painel',
  imports: [],
  templateUrl: './painel.html',
  styleUrl: './painel.scss',
})
export class Painel {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly sessao = this.auth.sessao;

  constructor() {
    this.auth.carregarSessao().subscribe({
      // O 401 ja e tratado pelo interceptor, que devolve para o login.
      error: () => undefined,
    });
  }

  sair(): void {
    this.auth.logout();
    void this.router.navigate(['/admin/login']);
  }
}
