import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { AdminArtigoService } from './admin-artigo.service';

describe('AdminArtigoService', () => {
  let servico: AdminArtigoService;
  let controlador: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servico = TestBed.inject(AdminArtigoService);
    controlador = TestBed.inject(HttpTestingController);
  });

  it('publicar usa PATCH na rota de publish', () => {
    servico.publicar('id-1').subscribe();

    const requisicao = controlador.expectOne((r) => r.url.endsWith('/admin/articles/id-1/publish'));
    expect(requisicao.request.method).toBe('PATCH');
  });

  it('despublicar usa PATCH na rota de unpublish', () => {
    servico.despublicar('id-1').subscribe();

    expect(
      controlador.expectOne((r) => r.url.endsWith('/admin/articles/id-1/unpublish')).request.method,
    ).toBe('PATCH');
  });

  it('o preview devolve só o html, não o envelope', () => {
    let html = '';
    servico.preview('# Oi').subscribe((resultado) => (html = resultado));

    controlador
      .expectOne((r) => r.url.endsWith('/admin/articles/preview'))
      .flush({ html: '<h1>Oi</h1>' });

    expect(html).toBe('<h1>Oi</h1>');
  });

  it('a imagem sobe como multipart, no campo que o backend espera', () => {
    const arquivo = new File(['bytes'], 'capa.png', { type: 'image/png' });
    servico.enviarImagem(arquivo).subscribe();

    const requisicao = controlador.expectOne((r) => r.url.endsWith('/admin/images'));
    const corpo = requisicao.request.body as FormData;

    expect(requisicao.request.method).toBe('POST');
    expect(corpo.get('arquivo')).toBeInstanceOf(File);
  });

  it('criar manda POST e editar manda PUT', () => {
    const artigo = {
      title: 'T', summary: 'r', contentMarkdown: 't',
      coverImageUrl: null, game: null, score: null, tags: [],
    };

    servico.criar(artigo).subscribe();
    expect(controlador.expectOne((r) => r.url.endsWith('/admin/articles')).request.method).toBe('POST');

    servico.atualizar('id-1', artigo).subscribe();
    expect(controlador.expectOne((r) => r.url.endsWith('/admin/articles/id-1')).request.method).toBe('PUT');
  });
});
