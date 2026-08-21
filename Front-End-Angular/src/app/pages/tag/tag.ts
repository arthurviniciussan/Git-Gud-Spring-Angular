import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { CardDeArtigo } from '../../components/card-de-artigo/card-de-artigo';
import { EstadoVazio } from '../../components/estado-vazio/estado-vazio';
import { Paginacao } from '../../components/paginacao/paginacao';
import { ArtigoService } from '../../services/artigo.service';
import { Pagina, ResumoDeArtigo } from '../../types/artigo.type';

/** Artigos de um assunto. Mesma grade da home, com o filtro fixo pela URL. */
@Component({
  selector: 'app-tag',
  imports: [CardDeArtigo, Paginacao, EstadoVazio, RouterLink],
  templateUrl: './tag.html',
  styleUrl: './tag.scss',
})
export class Tag {
  private readonly artigos = inject(ArtigoService);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly carregando = signal(true);
  readonly pagina = signal<Pagina<ResumoDeArtigo> | null>(null);

  readonly slug = toSignal(this.rota.paramMap.pipe(map((p) => p.get('slug') ?? '')), {
    initialValue: '',
  });

  readonly vazio = computed(() => !this.carregando() && this.pagina()?.content.length === 0);

  constructor() {
    this.rota.paramMap.subscribe(() => this.carregar());
    this.rota.queryParamMap.subscribe(() => this.carregar());
  }

  irParaPagina(pagina: number): void {
    void this.router.navigate([], {
      relativeTo: this.rota,
      queryParams: { page: pagina || null },
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private carregar(): void {
    const pagina = Number(this.rota.snapshot.queryParamMap.get('page') ?? 0);

    this.carregando.set(true);
    this.artigos.listar({ tag: this.slug(), pagina, tamanho: 9 }).subscribe({
      next: (resposta) => {
        this.pagina.set(resposta);
        this.carregando.set(false);
      },
      error: () => {
        this.pagina.set(null);
        this.carregando.set(false);
      },
    });
  }
}
