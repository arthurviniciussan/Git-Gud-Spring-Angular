import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { DefaultLoginLayout } from './default-login-layout';

describe('DefaultLoginLayout', () => {
  let fixture: ComponentFixture<DefaultLoginLayout>;

  const montar = (entradas: Record<string, unknown> = {}) => {
    for (const [nome, valor] of Object.entries(entradas)) {
      fixture.componentRef.setInput(nome, valor);
    }
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [DefaultLoginLayout] }).compileComponents();
    fixture = TestBed.createComponent(DefaultLoginLayout);
  });

  it('sem texto no botão secundário, ele e o separador somem', () => {
    const elemento = montar({ primaryBtnText: 'entrar' });

    // O blog nao tem cadastro: nao ha para onde mandar o visitante.
    expect(elemento.querySelector('.botao--secundario')).toBeNull();
    expect(elemento.querySelector('.separador')).toBeNull();
  });

  it('com texto, o botão secundário aparece', () => {
    const elemento = montar({ primaryBtnText: 'entrar', secondaryBtnText: 'criar conta' });

    expect(elemento.querySelector('.botao--secundario')?.textContent?.trim()).toBe('criar conta');
  });

  it('o botão principal se liga ao formulário projetado, para o Enter funcionar', () => {
    const botao = montar({ formularioId: 'formulario-de-login' }).querySelector(
      '.botao--primario',
    ) as HTMLButtonElement;

    // O botao fica FORA do <form> (que entra por ng-content). Sem o atributo
    // `form`, o Enter no campo nao enviaria nada.
    expect(botao.getAttribute('form')).toBe('formulario-de-login');
    expect(botao.getAttribute('type')).toBe('submit');
  });

  it('sem formulário associado, o botão não finge ser submit', () => {
    const botao = montar({}).querySelector('.botao--primario') as HTMLButtonElement;

    expect(botao.getAttribute('form')).toBeNull();
    expect(botao.getAttribute('type')).toBe('button');
  });
});
