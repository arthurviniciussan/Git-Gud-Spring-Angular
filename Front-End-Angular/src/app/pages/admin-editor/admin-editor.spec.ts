import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminEditor } from './admin-editor';

const ARTIGO = {
  id: 'a1',
  slug: 'elden-ring',
  title: 'Elden Ring é difícil',
  summary: 'E tudo bem.',
  contentMarkdown: '# Texto',
  coverImageUrl: null,
  game: 'Elden Ring',
  score: 9.5,
  status: 'PUBLISHED' as const,
  publishedAt: '2026-08-01T10:00:00Z',
  createdAt: '2026-08-01T10:00:00Z',
  updatedAt: '2026-08-01T10:00:00Z',
  tags: [{ name: 'RPG', slug: 'rpg' }],
};

describe('AdminEditor', () => {
  let fixture: ComponentFixture<AdminEditor>;
  let componente: AdminEditor;
  let controlador: HttpTestingController;

  const montar = (id: string | null) => {
    TestBed.configureTestingModule({
      imports: [AdminEditor],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: ToastrService, useValue: { success: vi.fn(), error: vi.fn() } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => id } } },
        },
      ],
    });

    fixture = TestBed.createComponent(AdminEditor);
    componente = fixture.componentInstance;
    controlador = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('abrir um artigo existente preenche o formulário', () => {
    montar('a1');
    controlador.expectOne((r) => r.url.endsWith('/admin/articles/a1')).flush(ARTIGO);

    expect(componente.formulario.controls.title.value).toBe('Elden Ring é difícil');
    expect(componente.formulario.controls.tags.value).toBe('RPG');
  });

  it('abrir um artigo não inventa aviso de rascunho não salvo', () => {
    montar('a1');
    controlador.expectOne((r) => r.url.endsWith('/admin/articles/a1')).flush(ARTIGO);

    // Regressão: carregar dispara valueChanges. Sem guarda, o editor gravava um
    // rascunho na abertura e logo oferecia de volta o que ele mesmo escreveu.
    expect(componente.rascunhoRecuperavel()).toBeNull();
    expect(componente.salvo()).toBe(true);
    expect(componente.temAlteracaoNaoSalva()).toBe(false);
  });

  it('digitar marca como não salvo e guarda o rascunho no navegador', () => {
    montar('a1');
    controlador.expectOne((r) => r.url.endsWith('/admin/articles/a1')).flush(ARTIGO);

    componente.textoMudou('# Texto novo');

    expect(componente.salvo()).toBe(false);
    expect(componente.temAlteracaoNaoSalva()).toBe(true);
    expect(localStorage.getItem('gitgud.rascunho.a1')).toContain('Texto novo');
  });

  it('artigo novo com rascunho guardado oferece recuperar', () => {
    localStorage.setItem(
      'gitgud.rascunho.novo',
      JSON.stringify({
        artigo: {
          title: 'Interrompido', summary: 'r', contentMarkdown: '# Meio do texto',
          coverImageUrl: null, game: null, score: null, tags: [],
        },
        em: '2026-08-21T12:00:00Z',
      }),
    );

    montar(null);

    expect(componente.rascunhoRecuperavel()?.title).toBe('Interrompido');

    componente.restaurarRascunho();
    expect(componente.formulario.controls.title.value).toBe('Interrompido');
  });

  it('formulário incompleto não chega a chamar a API', () => {
    montar(null);
    componente.formulario.patchValue({ title: '', summary: '', contentMarkdown: '' });

    componente.salvar();

    controlador.expectNone((r) => r.url.endsWith('/admin/articles'));
  });

  it('salvar limpa o rascunho local e volta ao estado salvo', () => {
    montar(null);
    componente.formulario.markAsDirty();
    componente.formulario.patchValue({
      title: 'Novo artigo', summary: 'resumo', contentMarkdown: '# Oi', tags: 'RPG, Indie',
    });

    componente.salvar();

    const requisicao = controlador.expectOne((r) => r.url.endsWith('/admin/articles'));
    // As tags vão como lista, não como o texto separado por vírgula do campo.
    expect(requisicao.request.body.tags).toEqual(['RPG', 'Indie']);

    requisicao.flush({ ...ARTIGO, id: 'novo-id', status: 'DRAFT' });

    expect(componente.salvo()).toBe(true);
    expect(localStorage.getItem('gitgud.rascunho.novo')).toBeNull();
  });

  it('salvar e publicar encadeia as duas chamadas', () => {
    montar(null);
    componente.formulario.markAsDirty();
    componente.formulario.patchValue({
      title: 'Novo', summary: 'resumo', contentMarkdown: '# Oi',
    });

    componente.salvar(true);

    controlador
      .expectOne((r) => r.url.endsWith('/admin/articles'))
      .flush({ ...ARTIGO, id: 'novo-id', status: 'DRAFT' });

    controlador.expectOne((r) => r.url.endsWith('/admin/articles/novo-id/publish')).flush({
      ...ARTIGO, id: 'novo-id', status: 'PUBLISHED',
    });

    expect(componente.publicado()).toBe(true);
  });
});
