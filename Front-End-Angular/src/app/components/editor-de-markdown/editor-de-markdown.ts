import {
  Component,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { AdminArtigoService } from '../../services/admin-artigo.service';

type Atalho = {
  rotulo: string;
  titulo: string;
  antes: string;
  depois: string;
  exemplo: string;
};

/**
 * Escrita em Markdown com preview ao lado.
 *
 * <p>O preview vem do servidor, pela mesma conversao que o artigo salvo recebe.
 * Renderizar aqui com outra biblioteca mostraria uma coisa e publicaria outra.
 */
@Component({
  selector: 'app-editor-de-markdown',
  imports: [],
  templateUrl: './editor-de-markdown.html',
  styleUrl: './editor-de-markdown.scss',
})
export class EditorDeMarkdown {
  /** Quanto tempo esperar parado antes de pedir o preview ao servidor. */
  private static readonly ESPERA_DO_PREVIEW = 400;

  private readonly admin = inject(AdminArtigoService);

  readonly texto = input('');
  readonly textoMudou = output<string>();

  private readonly area = viewChild.required<ElementRef<HTMLTextAreaElement>>('area');

  readonly html = signal('');
  readonly enviandoImagem = signal(false);
  readonly erroDeImagem = signal<string | null>(null);
  readonly arrastando = signal(false);

  readonly contagem = computed(() => {
    const limpo = this.texto().trim();
    const palavras = limpo ? limpo.split(/\s+/).length : 0;
    // ~200 palavras por minuto é a média de leitura em português.
    return { palavras, minutos: Math.max(1, Math.round(palavras / 200)) };
  });

  readonly atalhos: Atalho[] = [
    { rotulo: 'H2', titulo: 'Título de seção', antes: '## ', depois: '', exemplo: 'Título' },
    { rotulo: 'B', titulo: 'Negrito', antes: '**', depois: '**', exemplo: 'texto' },
    { rotulo: 'I', titulo: 'Itálico', antes: '*', depois: '*', exemplo: 'texto' },
    { rotulo: '“”', titulo: 'Citação', antes: '> ', depois: '', exemplo: 'citação' },
    { rotulo: '•', titulo: 'Lista', antes: '- ', depois: '', exemplo: 'item' },
    { rotulo: '</>', titulo: 'Código', antes: '`', depois: '`', exemplo: 'codigo' },
    { rotulo: '🔗', titulo: 'Link', antes: '[', depois: '](https://)', exemplo: 'texto' },
  ];

  private temporizador?: ReturnType<typeof setTimeout>;

  constructor() {
    effect(() => {
      const markdown = this.texto();
      clearTimeout(this.temporizador);

      if (!markdown.trim()) {
        this.html.set('');
        return;
      }

      // Debounce: sem isso, cada tecla viraria uma chamada ao servidor.
      this.temporizador = setTimeout(() => {
        this.admin.preview(markdown).subscribe({
          next: (html) => this.html.set(html),
          error: () => this.html.set('<p><em>Não foi possível gerar o preview.</em></p>'),
        });
      }, EditorDeMarkdown.ESPERA_DO_PREVIEW);
    });
  }

  aoDigitar(evento: Event): void {
    this.textoMudou.emit((evento.target as HTMLTextAreaElement).value);
  }

  /** Envolve a seleção com a marcação, ou insere um exemplo se nada estiver selecionado. */
  aplicar(atalho: Atalho): void {
    const area = this.area().nativeElement;
    const { selectionStart: inicio, selectionEnd: fim, value } = area;
    const selecionado = value.slice(inicio, fim) || atalho.exemplo;

    const novo =
      value.slice(0, inicio) + atalho.antes + selecionado + atalho.depois + value.slice(fim);

    this.textoMudou.emit(novo);

    // Devolve o cursor para dentro da marcação, senão escrever fica desconfortável.
    queueMicrotask(() => {
      area.focus();
      area.setSelectionRange(inicio + atalho.antes.length, inicio + atalho.antes.length + selecionado.length);
    });
  }

  aoSoltar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastando.set(false);

    const arquivos = Array.from(evento.dataTransfer?.files ?? []);
    this.enviarImagens(arquivos.filter((arquivo) => arquivo.type.startsWith('image/')));
  }

  aoArrastarSobre(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastando.set(true);
  }

  aoSairDoArraste(): void {
    this.arrastando.set(false);
  }

  /** Print colado com Ctrl+V vira upload — o caminho mais curto para um blog de jogos. */
  aoColar(evento: ClipboardEvent): void {
    const imagens = Array.from(evento.clipboardData?.items ?? [])
      .filter((item) => item.type.startsWith('image/'))
      .map((item) => item.getAsFile())
      .filter((arquivo): arquivo is File => arquivo !== null);

    if (imagens.length) {
      // Só bloqueia o padrão quando há imagem: colar texto precisa continuar
      // funcionando normalmente.
      evento.preventDefault();
      this.enviarImagens(imagens);
    }
  }

  private enviarImagens(arquivos: File[]): void {
    if (!arquivos.length) {
      return;
    }

    this.enviandoImagem.set(true);
    this.erroDeImagem.set(null);

    const [primeiro, ...resto] = arquivos;

    this.admin.enviarImagem(primeiro).subscribe({
      next: (imagem) => {
        this.inserirNoCursor(`\n![](${imagem.url})\n`);
        this.enviandoImagem.set(false);
        this.enviarImagens(resto);
      },
      error: (falha: { error?: { message?: string } }) => {
        this.enviandoImagem.set(false);
        this.erroDeImagem.set(falha.error?.message ?? 'Não foi possível enviar a imagem.');
      },
    });
  }

  private inserirNoCursor(trecho: string): void {
    const area = this.area().nativeElement;
    const posicao = area.selectionStart;
    const valor = area.value;

    this.textoMudou.emit(valor.slice(0, posicao) + trecho + valor.slice(posicao));

    queueMicrotask(() => {
      area.focus();
      area.setSelectionRange(posicao + trecho.length, posicao + trecho.length);
    });
  }
}
