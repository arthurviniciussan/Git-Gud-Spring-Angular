import { DatePipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ResumoDeArtigo } from '../../types/artigo.type';

/**
 * Card da listagem: capa do jogo com o titulo por cima.
 *
 * <p>O card inteiro e clicavel por um link esticado sobre ele, mas o link fica
 * no titulo — assim o nome acessivel do destino e o titulo do artigo, e nao
 * "leia mais". As tags ficam acima do link esticado para continuarem clicaveis.
 */
@Component({
  selector: 'app-card-de-artigo',
  imports: [RouterLink, DatePipe],
  templateUrl: './card-de-artigo.html',
  styleUrl: './card-de-artigo.scss',
})
export class CardDeArtigo {
  readonly artigo = input.required<ResumoDeArtigo>();

  /** O primeiro card da home ganha prioridade de carregamento; o resto e preguicoso. */
  readonly prioritario = input(false);

  readonly temCapa = computed(() => !!this.artigo().coverImageUrl);

  /** Sem capa, a inicial do jogo (ou do titulo) preenche o espaco. */
  readonly inicial = computed(() => {
    const artigo = this.artigo();
    return (artigo.game ?? artigo.title).trim().charAt(0).toUpperCase();
  });

  readonly nota = computed(() => {
    const nota = this.artigo().score;
    return nota === null ? null : nota.toFixed(1).replace('.', ',');
  });
}
