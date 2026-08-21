import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TemaService } from './tema.service';

/** Finge a resposta do sistema para prefers-color-scheme. */
function sistemaEscuro(escuro: boolean): void {
  vi.stubGlobal('matchMedia', (consulta: string) => ({
    matches: escuro && consulta.includes('dark'),
    media: consulta,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  }));
}

describe('TemaService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-tema');
    TestBed.resetTestingModule();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  const criar = () => TestBed.inject(TemaService);

  it('sem escolha guardada, segue o sistema', () => {
    sistemaEscuro(true);

    const tema = criar();

    expect(tema.preferencia()).toBe('sistema');
    expect(tema.temaEfetivo()).toBe('escuro');
    // Sem atributo, quem decide e o prefers-color-scheme no CSS.
    expect(document.documentElement.hasAttribute('data-tema')).toBe(false);
  });

  it('a escolha manual vence a preferencia do sistema', () => {
    sistemaEscuro(true);

    const tema = criar();
    tema.definir('claro');

    expect(tema.temaEfetivo()).toBe('claro');
    expect(document.documentElement.getAttribute('data-tema')).toBe('claro');
  });

  it('alternar parte do tema que esta valendo, nao do padrao', () => {
    sistemaEscuro(true);

    const tema = criar();
    tema.alternar();

    // Sistema escuro + alternar = claro. Se partisse do padrao "claro",
    // o primeiro clique nao mudaria nada na tela.
    expect(tema.temaEfetivo()).toBe('claro');
  });

  it('a escolha sobrevive a um recarregamento', () => {
    sistemaEscuro(false);
    criar().definir('escuro');

    TestBed.resetTestingModule();
    expect(criar().temaEfetivo()).toBe('escuro');
  });

  it('voltar para "sistema" apaga a escolha guardada', () => {
    sistemaEscuro(true);
    const tema = criar();

    tema.definir('claro');
    tema.definir('sistema');

    expect(localStorage.getItem('gitgud.tema')).toBeNull();
    expect(tema.temaEfetivo()).toBe('escuro');
  });

  it('nao quebra quando o armazenamento esta bloqueado', () => {
    sistemaEscuro(false);
    const original = Storage.prototype.setItem;
    Storage.prototype.setItem = () => {
      throw new Error('bloqueado');
    };

    try {
      const tema = criar();
      expect(() => tema.definir('escuro')).not.toThrow();
      // Sem poder guardar, o tema ainda vale para esta navegacao.
      expect(tema.temaEfetivo()).toBe('escuro');
    } finally {
      Storage.prototype.setItem = original;
    }
  });
});
