import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { beforeEach, describe, expect, it } from 'vitest';
import { PrimaryInput } from './primary-input';

@Component({
  imports: [PrimaryInput, ReactiveFormsModule],
  template: `
    <form [formGroup]="formulario">
      <app-primary-input
        formControlName="email"
        inputName="email"
        type="email"
        label="Email"
        autocomplete="username"
      />
    </form>
  `,
})
class Hospedeiro {
  readonly formulario = new FormGroup({ email: new FormControl('') });
}

describe('PrimaryInput', () => {
  let fixture: ComponentFixture<Hospedeiro>;
  let elemento: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Hospedeiro] }).compileComponents();
    fixture = TestBed.createComponent(Hospedeiro);
    fixture.detectChanges();
    elemento = fixture.nativeElement as HTMLElement;
  });

  it('o rótulo aponta para o campo', () => {
    const rotulo = elemento.querySelector('label') as HTMLLabelElement;
    const campo = elemento.querySelector('input') as HTMLInputElement;

    // Antes o input nao tinha id: o `for` do label nao apontava para nada e
    // clicar no rotulo nao focava o campo.
    expect(campo.id).toBe('email');
    expect(rotulo.getAttribute('for')).toBe('email');
  });

  it('declara o autocomplete para o gerenciador de senhas reconhecer o campo', () => {
    expect(elemento.querySelector('input')?.getAttribute('autocomplete')).toBe('username');
  });

  it('digitar no campo atualiza o formulário', () => {
    const campo = elemento.querySelector('input') as HTMLInputElement;

    campo.value = 'admin@gitgud.dev';
    campo.dispatchEvent(new Event('input'));

    expect(fixture.componentInstance.formulario.value.email).toBe('admin@gitgud.dev');
  });

  it('o valor do formulário aparece no campo', () => {
    fixture.componentInstance.formulario.setValue({ email: 'arthur@gitgud.dev' });
    fixture.detectChanges();

    expect((elemento.querySelector('input') as HTMLInputElement).value).toBe('arthur@gitgud.dev');
  });

  it('desabilitar o controle desabilita o campo', () => {
    fixture.componentInstance.formulario.controls.email.disable();
    fixture.detectChanges();

    expect((elemento.querySelector('input') as HTMLInputElement).disabled).toBe(true);
  });
});
