import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { EnvioDeArtigo } from '../types/artigo.type';
import { RascunhoLocalService } from './rascunho-local.service';

describe('RascunhoLocalService', () => {
  let servico: RascunhoLocalService;

  const artigo = (): EnvioDeArtigo => ({
    title: 'Elden Ring',
    summary: 'resumo',
    contentMarkdown: '# Texto',
    coverImageUrl: null,
    game: 'Elden Ring',
    score: 9.5,
    tags: ['RPG'],
  });

  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
    servico = TestBed.inject(RascunhoLocalService);
  });

  it('devolve o que foi guardado', () => {
    servico.guardar('novo', artigo());

    expect(servico.recuperar('novo')?.artigo.title).toBe('Elden Ring');
  });

  it('cada artigo tem seu próprio rascunho', () => {
    servico.guardar('artigo-1', { ...artigo(), title: 'Primeiro' });
    servico.guardar('artigo-2', { ...artigo(), title: 'Segundo' });

    // Sem separar por id, editar dois artigos misturaria os rascunhos.
    expect(servico.recuperar('artigo-1')?.artigo.title).toBe('Primeiro');
    expect(servico.recuperar('artigo-2')?.artigo.title).toBe('Segundo');
  });

  it('sem rascunho guardado, devolve nulo', () => {
    expect(servico.recuperar('nao-existe')).toBeNull();
  });

  it('descartar apaga o rascunho', () => {
    servico.guardar('novo', artigo());
    servico.descartar('novo');

    expect(servico.recuperar('novo')).toBeNull();
  });

  it('rascunho corrompido não impede de abrir o editor', () => {
    localStorage.setItem('gitgud.rascunho.novo', '{isto nao e json');

    expect(servico.recuperar('novo')).toBeNull();
    // E se limpa sozinho, para não repetir o erro na próxima abertura.
    expect(localStorage.getItem('gitgud.rascunho.novo')).toBeNull();
  });

  it('não quebra quando o armazenamento está bloqueado', () => {
    const original = Storage.prototype.setItem;
    Storage.prototype.setItem = () => {
      throw new Error('bloqueado');
    };

    try {
      expect(() => servico.guardar('novo', artigo())).not.toThrow();
    } finally {
      Storage.prototype.setItem = original;
    }
  });
});
