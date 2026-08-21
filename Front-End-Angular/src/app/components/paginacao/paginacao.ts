import { Component, computed, input, output } from '@angular/core';

/**
 * Paginacao simples: anterior, indicador, proxima.
 *
 * <p>Nao lista todos os numeros de proposito — com um blog pessoal, uma fileira
 * de numeros ocuparia mais espaco do que resolve.
 */
@Component({
  selector: 'app-paginacao',
  imports: [],
  templateUrl: './paginacao.html',
  styleUrl: './paginacao.scss',
})
export class Paginacao {
  readonly paginaAtual = input.required<number>();
  readonly totalDePaginas = input.required<number>();

  readonly mudou = output<number>();

  readonly temAnterior = computed(() => this.paginaAtual() > 0);
  readonly temProxima = computed(() => this.paginaAtual() < this.totalDePaginas() - 1);

  anterior(): void {
    if (this.temAnterior()) {
      this.mudou.emit(this.paginaAtual() - 1);
    }
  }

  proxima(): void {
    if (this.temProxima()) {
      this.mudou.emit(this.paginaAtual() + 1);
    }
  }
}
