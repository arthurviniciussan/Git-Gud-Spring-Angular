import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { adminGuard } from './admin.guard';

describe('adminGuard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: Router,
          // Devolve os comandos crus para o teste poder inspecionar o destino.
          useValue: { createUrlTree: (comandos: string[]) => comandos as unknown as UrlTree },
        },
      ],
    });
    sessionStorage.clear();
  });

  const executar = () =>
    TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  it('deixa entrar quando ha token', () => {
    sessionStorage.setItem('gitgud.token', 'token-de-teste');

    expect(executar()).toBe(true);
  });

  it('manda para o login quando nao ha token', () => {
    expect(executar()).toEqual(['/admin/login']);
  });
});
