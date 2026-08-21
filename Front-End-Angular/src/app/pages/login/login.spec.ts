import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let componente: Login;
  let controlador: HttpTestingController;
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    navigate = vi.fn();

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate } },
        { provide: ToastrService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    componente = fixture.componentInstance;
    controlador = TestBed.inject(HttpTestingController);
    sessionStorage.clear();
    fixture.detectChanges();
  });

  const preencher = () =>
    componente.loginForm.setValue({ email: 'admin@gitgud.dev', password: 'SenhaDeTeste123!' });

  it('entra no painel quando a API confirma o login', () => {
    preencher();
    componente.submit();

    controlador.expectOne((r) => r.url.endsWith('/auth/login')).flush({
      name: 'Arthur',
      email: 'admin@gitgud.dev',
      role: 'ADMIN',
      token: 'token-de-teste',
    });

    expect(navigate).toHaveBeenCalledWith(['/admin']);
    expect(sessionStorage.getItem('gitgud.token')).toBe('token-de-teste');
  });

  it('nao navega quando a senha esta errada', () => {
    preencher();
    componente.submit();

    controlador
      .expectOne((r) => r.url.endsWith('/auth/login'))
      .flush({ message: 'Credenciais invalidas.' }, { status: 401, statusText: 'Unauthorized' });

    // Regressao: a navegacao ficava fora do subscribe e entrava no painel
    // mesmo com credencial recusada.
    expect(navigate).not.toHaveBeenCalled();
    expect(componente.erro()).toBe('Credenciais invalidas.');
  });

  it('mostra a mensagem de bloqueio por tentativas demais', () => {
    preencher();
    componente.submit();

    controlador
      .expectOne((r) => r.url.endsWith('/auth/login'))
      .flush(
        { message: 'Tentativas de login demais. Aguarde 15 minutos.' },
        { status: 429, statusText: 'Too Many Requests' },
      );

    expect(componente.erro()).toContain('Aguarde 15 minutos');
  });

  it('nao chama a API com formulario invalido', () => {
    componente.loginForm.setValue({ email: 'nao-e-email', password: 'curta' });
    componente.submit();

    controlador.expectNone(() => true);
  });
});
