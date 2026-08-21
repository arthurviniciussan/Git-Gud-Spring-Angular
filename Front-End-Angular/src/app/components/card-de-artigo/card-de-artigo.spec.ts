import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { ResumoDeArtigo } from '../../types/artigo.type';
import { CardDeArtigo } from './card-de-artigo';

describe('CardDeArtigo', () => {
  let fixture: ComponentFixture<CardDeArtigo>;

  const artigo = (extras: Partial<ResumoDeArtigo> = {}): ResumoDeArtigo => ({
    slug: 'elden-ring-e-dificil',
    title: 'Elden Ring é difícil',
    summary: 'E tudo bem que seja.',
    coverImageUrl: '/uploads/2026/08/capa.webp',
    game: 'Elden Ring',
    score: 9.5,
    publishedAt: '2026-08-01T10:00:00Z',
    tags: [{ name: 'RPG', slug: 'rpg' }],
    ...extras,
  });

  const montar = (dados: ResumoDeArtigo) => {
    fixture.componentRef.setInput('artigo', dados);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CardDeArtigo],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(CardDeArtigo);
  });

  it('leva o título para o endereço do artigo', () => {
    const elemento = montar(artigo());
    const link = elemento.querySelector('.card__titulo a') as HTMLAnchorElement;

    expect(link.textContent?.trim()).toBe('Elden Ring é difícil');
    // O nome acessivel do link precisa ser o titulo, nao "leia mais".
    expect(link.getAttribute('href')).toBe('/artigo/elden-ring-e-dificil');
  });

  it('mostra a capa quando o artigo tem uma', () => {
    const elemento = montar(artigo());

    expect(elemento.querySelector('.card__capa img')?.getAttribute('src')).toBe(
      '/uploads/2026/08/capa.webp',
    );
    expect(elemento.querySelector('.card__sem-capa')).toBeNull();
  });

  it('sem capa, cai na inicial do jogo em vez de deixar buraco', () => {
    const elemento = montar(artigo({ coverImageUrl: null }));

    expect(elemento.querySelector('.card__capa img')).toBeNull();
    expect(elemento.querySelector('.card__sem-capa')?.textContent?.trim()).toBe('E');
  });

  it('formata a nota no padrão brasileiro', () => {
    expect(montar(artigo({ score: 9.5 })).querySelector('.card__nota')?.textContent).toContain(
      '9,5',
    );
  });

  it('artigo sem nota não mostra selo', () => {
    expect(montar(artigo({ score: null })).querySelector('.card__nota')).toBeNull();
  });

  it('as tags continuam clicáveis, apesar do link esticado sobre o card', () => {
    const elemento = montar(artigo());
    const tag = elemento.querySelector('.card__tags a') as HTMLAnchorElement;

    expect(tag.getAttribute('href')).toBe('/tag/rpg');
  });

  it('só o primeiro card carrega a imagem com prioridade', () => {
    fixture.componentRef.setInput('artigo', artigo());
    fixture.componentRef.setInput('prioritario', true);
    fixture.detectChanges();

    const imagem = (fixture.nativeElement as HTMLElement).querySelector('.card__capa img');
    expect(imagem?.getAttribute('loading')).toBe('eager');
    expect(imagem?.getAttribute('fetchpriority')).toBe('high');
  });

  it('os demais cards carregam a imagem preguiçosamente', () => {
    const elemento = montar(artigo());
    const imagem = elemento.querySelector('.card__capa img');

    expect(imagem?.getAttribute('loading')).toBe('lazy');
    expect(imagem?.getAttribute('fetchpriority')).toBeNull();
  });
});
