import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { TemaService } from '../../services/tema.service';

/**
 * Barra fixa do blog: identidade a esquerda, ferramentas a direita.
 *
 * <p>A busca navega para a home com ?q=, em vez de guardar estado interno —
 * assim o resultado tem endereco proprio, da para compartilhar e o botao voltar
 * do navegador funciona.
 */
@Component({
  selector: 'app-cabecalho-do-blog',
  imports: [ReactiveFormsModule, RouterLink, RouterLinkActive],
  templateUrl: './cabecalho-do-blog.html',
  styleUrl: './cabecalho-do-blog.scss',
})
export class CabecalhoDoBlog {
  private readonly router = inject(Router);
  private readonly tema = inject(TemaService);

  readonly logo = 'assets/log-Git-Gud!.png';
  readonly perfilNoGitHub = 'https://github.com/arthurviniciussan';

  readonly termo = new FormControl('', { nonNullable: true });
  readonly buscaAberta = signal(false);
  readonly temaEfetivo = this.tema.temaEfetivo;

  buscar(): void {
    const termo = this.termo.value.trim();

    void this.router.navigate(['/'], {
      queryParams: { q: termo || null },
      queryParamsHandling: 'merge',
    });
    this.buscaAberta.set(false);
  }

  alternarBusca(): void {
    this.buscaAberta.update((aberta) => !aberta);
  }

  alternarTema(): void {
    this.tema.alternar();
  }
}
