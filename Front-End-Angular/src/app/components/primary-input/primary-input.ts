import { Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

type TipoDeCampo = 'text' | 'email' | 'password';

/**
 * Campo de formulario com rotulo e icone.
 *
 * <p>Implementa ControlValueAccessor, entao funciona direto com formControlName.
 */
@Component({
  selector: 'app-primary-input',
  imports: [ReactiveFormsModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PrimaryInput),
      multi: true,
    },
  ],
  templateUrl: './primary-input.html',
  styleUrl: './primary-input.scss',
})
export class PrimaryInput implements ControlValueAccessor {
  readonly type = input<TipoDeCampo>('text');
  readonly placeholder = input('');
  readonly label = input('');
  readonly inputName = input('');

  /** Sem isto o gerenciador de senhas do navegador nao reconhece o campo. */
  readonly autocomplete = input('off');

  // Signals, e nao propriedades comuns: com change detection zoneless, escrever
  // num campo pelo codigo (form.setValue, carregar um rascunho para editar) nao
  // notificaria o template e o campo continuaria mostrando o valor antigo.
  readonly value = signal('');
  readonly desabilitado = signal(false);

  private aoMudar: (valor: string) => void = () => undefined;
  private aoTocar: () => void = () => undefined;

  onInput(evento: Event): void {
    this.aoMudar((evento.target as HTMLInputElement).value);
  }

  onBlur(): void {
    this.aoTocar();
  }

  writeValue(valor: string | null): void {
    this.value.set(valor ?? '');
  }

  registerOnChange(fn: (valor: string) => void): void {
    this.aoMudar = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.aoTocar = fn;
  }

  setDisabledState(desabilitado: boolean): void {
    this.desabilitado.set(desabilitado);
  }
}
