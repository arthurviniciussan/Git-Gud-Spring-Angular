import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { Home } from './home';

/** Mapa de query params controlavel pelo teste. */
function mapaDeParametros(valores: Record<string, string>) {
  return {
    get: (chave: string) => valores[chave] ?? null,
  };
}

describe('Home', () => {
  let fixture: ComponentFixture<Home>;
  let controlador: HttpTestingController;
  let queryParams: BehaviorSubject<ReturnType<typeof mapaDeParametros>>;

  const montar = (valores: Record<string, string> = {}) => {
    queryParams = new BehaviorSubject(mapaDeParametros(valores));

    TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { queryParamMap: queryParams.asObservable(), snapshot: { queryParamMap: mapaDeParametros(valores) } },
        },
      ],
    });

    fixture = TestBed.createComponent(Home);
    controlador = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  beforeEach(() => TestBed.resetTestingModule());

  const pedidoDeArtigos = () =>
    controlador.expectOne((r) => r.url.endsWith('/articles') && !r.url.endsWith('/tags'));

  it('carrega a primeira página ao abrir', () => {
    montar();

    expect(pedidoDeArtigos().request.params.get('page')).toBe('0');
  });

  it('a busca vem da URL, não de estado interno', () => {
    montar({ q: 'elden' });

    // Assim o resultado tem endereço próprio e o botão voltar funciona.
    expect(pedidoDeArtigos().request.params.get('q')).toBe('elden');
  });

  it('respeita a página pedida na URL', () => {
    montar({ page: '3' });

    expect(pedidoDeArtigos().request.params.get('page')).toBe('3');
  });

  it('mostra o estado vazio quando não há artigos', () => {
    montar();
    pedidoDeArtigos().flush({
      content: [], page: 0, size: 9, totalElements: 0, totalPages: 0, last: true,
    });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('app-estado-vazio')).not.toBeNull();
  });

  it('mostra recado próprio quando a API falha, em vez de tela vazia', () => {
    montar();
    pedidoDeArtigos().flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Não deu para carregar');
  });
});
