import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { AdminArtigoService } from '../../services/admin-artigo.service';
import { ArtigoDoPainel } from '../../types/artigo.type';

/** Lista de artigos do painel: rascunhos e publicados, com as ações de cada um. */
@Component({
  selector: 'app-admin-artigos',
  imports: [RouterLink, DatePipe],
  templateUrl: './admin-artigos.html',
  styleUrl: './admin-artigos.scss',
})
export class AdminArtigos {
  private readonly admin = inject(AdminArtigoService);
  private readonly toastr = inject(ToastrService);

  readonly carregando = signal(true);
  readonly artigos = signal<ArtigoDoPainel[]>([]);
  readonly ocupado = signal<string | null>(null);

  readonly rascunhos = computed(() => this.artigos().filter((a) => a.status === 'DRAFT').length);
  readonly publicados = computed(() => this.artigos().filter((a) => a.status === 'PUBLISHED').length);

  constructor() {
    this.carregar();
  }

  publicar(artigo: ArtigoDoPainel): void {
    this.executar(artigo, this.admin.publicar(artigo.id), 'Artigo publicado.');
  }

  despublicar(artigo: ArtigoDoPainel): void {
    this.executar(artigo, this.admin.despublicar(artigo.id), 'Artigo tirado do ar.');
  }

  excluir(artigo: ArtigoDoPainel): void {
    // Apagar não tem volta: vale confirmar antes.
    if (!confirm(`Apagar "${artigo.title}"? Isso não pode ser desfeito.`)) {
      return;
    }

    this.ocupado.set(artigo.id);
    this.admin.excluir(artigo.id).subscribe({
      next: () => {
        this.artigos.update((lista) => lista.filter((item) => item.id !== artigo.id));
        this.ocupado.set(null);
        this.toastr.success('Artigo apagado.');
      },
      error: () => {
        this.ocupado.set(null);
        this.toastr.error('Não foi possível apagar o artigo.');
      },
    });
  }

  private executar(
    artigo: ArtigoDoPainel,
    acao: ReturnType<AdminArtigoService['publicar']>,
    mensagem: string,
  ): void {
    this.ocupado.set(artigo.id);

    acao.subscribe({
      next: (atualizado) => {
        this.artigos.update((lista) =>
          lista.map((item) => (item.id === atualizado.id ? atualizado : item)),
        );
        this.ocupado.set(null);
        this.toastr.success(mensagem);
      },
      error: () => {
        this.ocupado.set(null);
        this.toastr.error('Não foi possível concluir a ação.');
      },
    });
  }

  private carregar(): void {
    this.admin.listar().subscribe({
      next: (pagina) => {
        this.artigos.set(pagina.content);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.toastr.error('Não foi possível carregar os artigos.');
      },
    });
  }
}
