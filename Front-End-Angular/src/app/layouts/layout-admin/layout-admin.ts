import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';

/** Moldura do painel: barra propria, sem o cabecalho do blog. */
@Component({
  selector: 'app-layout-admin',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './layout-admin.html',
  styleUrl: './layout-admin.scss',
})
export class LayoutAdmin {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly sessao = this.auth.sessao;

  constructor() {
    this.auth.carregarSessao().subscribe({
      // O 401 já é tratado pelo interceptor, que devolve para o login.
      error: () => undefined,
    });
  }

  sair(): void {
    this.auth.logout();
    void this.router.navigate(['/admin/login']);
  }
}
