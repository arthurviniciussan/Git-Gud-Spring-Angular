import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';
import { CardDeArtigo } from '../../components/card-de-artigo/card-de-artigo';
import { EstadoVazio } from '../../components/estado-vazio/estado-vazio';
import { ListaDeTags } from '../../components/lista-de-tags/lista-de-tags';
import { Paginacao } from '../../components/paginacao/paginacao';
import { ArtigoService } from '../../services/artigo.service';
import { Pagina, ResumoDeArtigo, Tag } from '../../types/artigo.type';

/**
 * A home do blog.
 *
 * <p>Pagina e busca vivem na URL, nao em estado interno: assim o resultado tem
 * endereco proprio, da para compartilhar e o botao voltar do navegador funciona.
 */
@Component({
  selector: 'app-home',
  imports: [CardDeArtigo, Paginacao, EstadoVazio, ListaDeTags],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private readonly artigos = inject(ArtigoService);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly carregando = signal(true);
  readonly falhou = signal(false);
  readonly pagina = signal<Pagina<ResumoDeArtigo> | null>(null);
  readonly tags = signal<Tag[]>([]);

  private readonly parametros = toSignal(
    this.rota.queryParamMap.pipe(
      map((mapa) => ({
        pagina: Number(mapa.get('page') ?? 0),
        busca: mapa.get('q') ?? '',
      })),
    ),
    { initialValue: { pagina: 0, busca: '' } },
  );

  readonly busca = computed(() => this.parametros().busca);
  readonly vazio = computed(() => !this.carregando() && this.pagina()?.content.length === 0);

  constructor() {
    this.artigos.tags().subscribe({
      next: (tags) => this.tags.set(tags),
      error: () => this.tags.set([]),
    });

    // Reage a cada mudanca de ?page= ou ?q= na URL.
    this.rota.queryParamMap.subscribe(() => this.carregar());
  }

  irParaPagina(pagina: number): void {
    void this.router.navigate([], {
      relativeTo: this.rota,
      queryParams: { page: pagina || null },
      queryParamsHandling: 'merge',
    });
    // Trocar de pagina sem voltar ao topo deixa o leitor no meio da lista nova.
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private carregar(): void {
    const { pagina, busca } = this.parametros();

    this.carregando.set(true);
    this.falhou.set(false);

    this.artigos.listar({ pagina, busca, tamanho: 9 }).subscribe({
      next: (resposta) => {
        this.pagina.set(resposta);
        this.carregando.set(false);
      },
      error: () => {
        this.falhou.set(true);
        this.carregando.set(false);
      },
    });
  }
}
