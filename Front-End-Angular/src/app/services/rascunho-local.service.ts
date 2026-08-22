import { Injectable } from '@angular/core';
import { EnvioDeArtigo } from '../types/artigo.type';

type RascunhoGuardado = {
  artigo: EnvioDeArtigo;
  em: string;
};

/**
 * Guarda o texto no navegador enquanto voce escreve.
 *
 * <p>Nao substitui o salvamento no servidor: serve para o caso de a aba fechar,
 * o note desligar ou o navegador travar no meio de um texto longo. Ao reabrir o
 * editor, o rascunho e oferecido de volta.
 *
 * <p>A chave inclui o id do artigo, entao editar dois artigos diferentes nao
 * mistura os rascunhos. Artigo novo usa a chave "novo".
 */
@Injectable({ providedIn: 'root' })
export class RascunhoLocalService {
  private static readonly PREFIXO = 'gitgud.rascunho.';

  guardar(chave: string, artigo: EnvioDeArtigo): void {
    this.escrever(chave, JSON.stringify({ artigo, em: new Date().toISOString() }));
  }

  recuperar(chave: string): RascunhoGuardado | null {
    const cru = this.ler(chave);
    if (!cru) {
      return null;
    }

    try {
      const guardado = JSON.parse(cru) as RascunhoGuardado;
      return guardado?.artigo ? guardado : null;
    } catch {
      // Rascunho corrompido não pode impedir de abrir o editor.
      this.descartar(chave);
      return null;
    }
  }

  descartar(chave: string): void {
    this.escrever(chave, null);
  }

  private ler(chave: string): string | null {
    try {
      return localStorage.getItem(RascunhoLocalService.PREFIXO + chave);
    } catch {
      return null;
    }
  }

  private escrever(chave: string, valor: string | null): void {
    try {
      if (valor === null) {
        localStorage.removeItem(RascunhoLocalService.PREFIXO + chave);
      } else {
        localStorage.setItem(RascunhoLocalService.PREFIXO + chave, valor);
      }
    } catch {
      // Armazenamento cheio ou bloqueado: seguir sem rascunho local é melhor
      // do que derrubar o editor.
    }
  }
}
