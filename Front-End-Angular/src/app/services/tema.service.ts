import { DOCUMENT } from '@angular/common';
import { computed, inject, Injectable, signal } from '@angular/core';

export type Tema = 'claro' | 'escuro';
export type PreferenciaDeTema = Tema | 'sistema';

/**
 * Tema claro/escuro.
 *
 * <p>O padrao e seguir o sistema. A escolha manual fica no localStorage e vence
 * a preferencia do sistema nos dois sentidos — quem escolhe claro num sistema
 * escuro continua no claro.
 *
 * <p>O atributo data-tema vai no <html>, e nao no <body>, porque e la que os
 * tokens de cor sao redefinidos.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  private static readonly CHAVE = 'gitgud.tema';

  private readonly documento = inject(DOCUMENT);

  private readonly preferenciaAtual = signal<PreferenciaDeTema>('sistema');

  readonly preferencia = this.preferenciaAtual.asReadonly();

  /** O tema que esta valendo agora, ja resolvendo "sistema". */
  readonly temaEfetivo = computed<Tema>(() => {
    const escolha = this.preferenciaAtual();
    return escolha === 'sistema' ? this.temaDoSistema() : escolha;
  });

  constructor() {
    this.preferenciaAtual.set(this.preferenciaGuardada());
    this.aplicar();
  }

  alternar(): void {
    this.definir(this.temaEfetivo() === 'escuro' ? 'claro' : 'escuro');
  }

  definir(preferencia: PreferenciaDeTema): void {
    this.preferenciaAtual.set(preferencia);
    this.guardar(preferencia);
    this.aplicar();
  }

  private aplicar(): void {
    const raiz = this.documento.documentElement;
    const escolha = this.preferenciaAtual();

    if (escolha === 'sistema') {
      // Sem o atributo, o CSS volta a decidir pelo prefers-color-scheme.
      raiz.removeAttribute('data-tema');
    } else {
      raiz.setAttribute('data-tema', escolha);
    }
  }

  private temaDoSistema(): Tema {
    // matchMedia nao existe na renderizacao no servidor (Etapa 5).
    if (typeof window === 'undefined' || !window.matchMedia) {
      return 'claro';
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'escuro' : 'claro';
  }

  private preferenciaGuardada(): PreferenciaDeTema {
    const guardada = this.ler();
    return guardada === 'claro' || guardada === 'escuro' ? guardada : 'sistema';
  }

  private ler(): string | null {
    try {
      return localStorage.getItem(TemaService.CHAVE);
    } catch {
      // Aba anonima com armazenamento bloqueado: sem preferencia guardada.
      return null;
    }
  }

  private guardar(preferencia: PreferenciaDeTema): void {
    try {
      if (preferencia === 'sistema') {
        localStorage.removeItem(TemaService.CHAVE);
      } else {
        localStorage.setItem(TemaService.CHAVE, preferencia);
      }
    } catch {
      // Sem armazenamento, o tema vale so para esta navegacao.
    }
  }
}
