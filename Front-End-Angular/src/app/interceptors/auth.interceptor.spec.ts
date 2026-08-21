import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controlador: HttpTestingController;
  let auth: AuthService;
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    navigate = vi.fn();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate } },
      ],
    });

    http = TestBed.inject(HttpClient);
    controlador = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    sessionStorage.clear();
  });

  it('anexa o token nas requisicoes quando ha sessao', () => {
    sessionStorage.setItem('gitgud.token', 'token-de-teste');

    http.get('/api/artigos').subscribe();

    const requisicao = controlador.expectOne('/api/artigos');
    expect(requisicao.request.headers.get('Authorization')).toBe('Bearer token-de-teste');
    requisicao.flush({});
  });

  it('nao inventa header quando nao ha sessao', () => {
    http.get('/api/artigos').subscribe();

    const requisicao = controlador.expectOne('/api/artigos');
    expect(requisicao.request.headers.has('Authorization')).toBe(false);
    requisicao.flush({});
  });

  it('limpa a sessao e volta ao login quando o token expira', () => {
    sessionStorage.setItem('gitgud.token', 'token-vencido');

    http.get('/api/admin/artigos').subscribe({ error: () => undefined });

    controlador
      .expectOne('/api/admin/artigos')
      .flush({ message: 'Autenticacao necessaria.' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.token).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/admin/login']);
  });

  it('nao redireciona quando o 401 e do proprio login', () => {
    http.post('/api/auth/login', {}).subscribe({ error: () => undefined });

    controlador
      .expectOne('/api/auth/login')
      .flush({ message: 'Credenciais invalidas.' }, { status: 401, statusText: 'Unauthorized' });

    // Senha errada precisa mostrar a mensagem na tela, nao recarregar o login.
    expect(navigate).not.toHaveBeenCalled();
  });
});
