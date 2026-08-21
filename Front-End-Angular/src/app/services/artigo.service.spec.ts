import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { ArtigoService } from './artigo.service';

describe('ArtigoService', () => {
  let servico: ArtigoService;
  let controlador: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servico = TestBed.inject(ArtigoService);
    controlador = TestBed.inject(HttpTestingController);
  });

  it('lista com pagina e tamanho padrao', () => {
    servico.listar().subscribe();

    const requisicao = controlador.expectOne((r) => r.url.endsWith('/articles'));
    expect(requisicao.request.params.get('page')).toBe('0');
    expect(requisicao.request.params.get('size')).toBe('9');
    expect(requisicao.request.params.has('tag')).toBe(false);
    expect(requisicao.request.params.has('q')).toBe(false);
  });

  it('manda a tag quando ha filtro por assunto', () => {
    servico.listar({ tag: 'rpg', pagina: 2 }).subscribe();

    const requisicao = controlador.expectOne((r) => r.url.endsWith('/articles'));
    expect(requisicao.request.params.get('tag')).toBe('rpg');
    expect(requisicao.request.params.get('page')).toBe('2');
  });

  it('manda o termo de busca como q', () => {
    servico.listar({ busca: 'elden' }).subscribe();

    expect(
      controlador.expectOne((r) => r.url.endsWith('/articles')).request.params.get('q'),
    ).toBe('elden');
  });

  it('busca vazia nao vira parametro na URL', () => {
    servico.listar({ busca: '' }).subscribe();

    expect(controlador.expectOne((r) => r.url.endsWith('/articles')).request.params.has('q')).toBe(
      false,
    );
  });

  it('busca o artigo pelo slug', () => {
    servico.porSlug('elden-ring').subscribe();

    controlador.expectOne((r) => r.url.endsWith('/articles/elden-ring'));
  });

  it('as chamadas publicas nao exigem token', () => {
    servico.listar().subscribe();

    // Sem sessao, nenhum Authorization e inventado — leitura e publica.
    expect(
      controlador.expectOne((r) => r.url.endsWith('/articles')).request.headers.has('Authorization'),
    ).toBe(false);
  });
});
