import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Meta, Title } from '@angular/platform-browser';
import { switchMap } from 'rxjs';
import { ListaDeTags } from '../../components/lista-de-tags/lista-de-tags';
import { ArtigoService } from '../../services/artigo.service';
import { Artigo as ArtigoModelo } from '../../types/artigo.type';

/**
 * A pagina de leitura.
 *
 * <p>O corpo entra por innerHTML sem sanitizacao adicional de proposito: o HTML
 * ja vem convertido e limpo pelo servidor (commonmark + jsoup), o que mantem
 * uma fonte unica de sanitizacao. Nao existe caminho pelo qual um visitante
 * consiga injetar conteudo aqui — so o admin escreve.
 */
@Component({
  selector: 'app-artigo',
  imports: [DatePipe, RouterLink, ListaDeTags],
  templateUrl: './artigo.html',
  styleUrl: './artigo.scss',
})
export class Artigo {
  private readonly artigos = inject(ArtigoService);
  private readonly rota = inject(ActivatedRoute);
  private readonly titulo = inject(Title);
  private readonly meta = inject(Meta);

  readonly carregando = signal(true);
  readonly naoEncontrado = signal(false);
  readonly artigo = signal<ArtigoModelo | null>(null);

  readonly nota = computed(() => {
    const nota = this.artigo()?.score;
    return nota === null || nota === undefined ? null : nota.toFixed(1).replace('.', ',');
  });

  constructor() {
    this.rota.paramMap
      .pipe(switchMap((parametros) => this.artigos.porSlug(parametros.get('slug') ?? '')))
      .subscribe({
        next: (artigo) => {
          this.artigo.set(artigo);
          this.carregando.set(false);
          this.descrever(artigo);
        },
        error: () => {
          this.naoEncontrado.set(true);
          this.carregando.set(false);
        },
      });
  }

  /**
   * Titulo e meta description por artigo.
   *
   * <p>Adianta parte da Etapa 5: sem isso, todo link compartilhado mostraria o
   * mesmo titulo generico. A renderizacao no servidor entra depois e faz o
   * Google enxergar isto tambem.
   */
  private descrever(artigo: ArtigoModelo): void {
    this.titulo.setTitle(`${artigo.title} — GitGud`);
    this.meta.updateTag({ name: 'description', content: artigo.summary });
    this.meta.updateTag({ property: 'og:title', content: artigo.title });
    this.meta.updateTag({ property: 'og:description', content: artigo.summary });
    this.meta.updateTag({ property: 'og:type', content: 'article' });

    if (artigo.coverImageUrl) {
      this.meta.updateTag({ property: 'og:image', content: artigo.coverImageUrl });
    }
  }
}
