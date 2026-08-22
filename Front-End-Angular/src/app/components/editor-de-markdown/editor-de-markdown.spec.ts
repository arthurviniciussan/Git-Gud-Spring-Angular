import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { EditorDeMarkdown } from './editor-de-markdown';

describe('EditorDeMarkdown', () => {
  let fixture: ComponentFixture<EditorDeMarkdown>;
  let componente: EditorDeMarkdown;
  let controlador: HttpTestingController;
  let emitido: string[];

  const area = () =>
    (fixture.nativeElement as HTMLElement).querySelector('textarea') as HTMLTextAreaElement;

  const comTexto = (texto: string) => {
    fixture.componentRef.setInput('texto', texto);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditorDeMarkdown],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(EditorDeMarkdown);
    componente = fixture.componentInstance;
    controlador = TestBed.inject(HttpTestingController);

    emitido = [];
    componente.textoMudou.subscribe((valor) => emitido.push(valor));
    fixture.detectChanges();
  });

  it('o atalho envolve o trecho selecionado', () => {
    comTexto('Elden Ring é difícil');
    area().setSelectionRange(0, 10);

    componente.aplicar({ rotulo: 'B', titulo: 'Negrito', antes: '**', depois: '**', exemplo: 'texto' });

    expect(emitido.at(-1)).toBe('**Elden Ring** é difícil');
  });

  it('sem seleção, o atalho insere um exemplo para não deixar marcação solta', () => {
    comTexto('');
    area().setSelectionRange(0, 0);

    componente.aplicar({ rotulo: 'H2', titulo: 'Título', antes: '## ', depois: '', exemplo: 'Título' });

    expect(emitido.at(-1)).toBe('## Título');
  });

  it('colar um print sobe a imagem e insere o Markdown no cursor', () => {
    comTexto('Antes');
    area().setSelectionRange(5, 5);

    const arquivo = new File(['bytes'], 'print.png', { type: 'image/png' });
    const evento = new Event('paste') as ClipboardEvent;
    Object.defineProperty(evento, 'clipboardData', {
      value: { items: [{ type: 'image/png', getAsFile: () => arquivo }] },
    });

    componente.aoColar(evento);

    controlador
      .expectOne((r) => r.url.endsWith('/admin/images'))
      .flush({ url: '/uploads/2026/08/abc.webp', largura: 1600, altura: 900, bytes: 1000 });

    expect(emitido.at(-1)).toBe('Antes\n![](/uploads/2026/08/abc.webp)\n');
  });

  it('colar texto comum não é interceptado', () => {
    const evento = new Event('paste') as ClipboardEvent;
    Object.defineProperty(evento, 'clipboardData', {
      value: { items: [{ type: 'text/plain', getAsFile: () => null }] },
    });

    componente.aoColar(evento);

    // Nenhum upload: colar texto precisa continuar funcionando normalmente.
    controlador.expectNone((r) => r.url.endsWith('/admin/images'));
    expect(evento.defaultPrevented).toBe(false);
  });

  it('falha no upload vira mensagem na tela, não erro silencioso', () => {
    const arquivo = new File(['bytes'], 'print.png', { type: 'image/png' });
    const evento = new Event('paste') as ClipboardEvent;
    Object.defineProperty(evento, 'clipboardData', {
      value: { items: [{ type: 'image/png', getAsFile: () => arquivo }] },
    });

    componente.aoColar(evento);

    controlador
      .expectOne((r) => r.url.endsWith('/admin/images'))
      .flush({ message: 'Arquivo grande demais.' }, { status: 400, statusText: 'Bad Request' });

    expect(componente.erroDeImagem()).toBe('Arquivo grande demais.');
    expect(componente.enviandoImagem()).toBe(false);
  });

  it('conta palavras e estima o tempo de leitura', () => {
    comTexto('uma '.repeat(400).trim());

    expect(componente.contagem().palavras).toBe(400);
    expect(componente.contagem().minutos).toBe(2);
  });
});
