import { Component, input, output } from '@angular/core';

/**
 * Moldura das telas de entrada: formulario a esquerda, identidade a direita.
 *
 * <p>O botao secundario e opcional. O blog nao tem cadastro, entao o login do
 * admin nao tem para onde mandar o visitante — mas manter o botao como opcao
 * preserva o componente reutilizavel.
 */
@Component({
  selector: 'app-default-login-layout',
  imports: [],
  templateUrl: './default-login-layout.html',
  styleUrl: './default-login-layout.scss',
})
export class DefaultLoginLayout {
  readonly logoImage = 'assets/log-Git-Gud!.png';
  readonly bonfireImage = '/assets/main-bonfire-image-removebg.png';

  readonly title = input('');
  readonly primaryBtnText = input('');
  /** Vazio esconde o botao secundario e o separador "or". */
  readonly secondaryBtnText = input('');
  readonly disablePrimaryBtn = input(true);

  /**
   * Id do formulario projetado.
   *
   * <p>O botao fica FORA do <form> (o form entra por ng-content), entao sem o
   * atributo `form` apontando para ele o Enter no campo nao enviaria nada — e
   * ninguem espera precisar clicar no botao para entrar.
   */
  readonly formularioId = input('');

  readonly onSubmit = output<void>({ alias: 'submit' });
  readonly onNavigate = output<void>({ alias: 'navigate' });

  submit(): void {
    this.onSubmit.emit();
  }

  navigate(): void {
    this.onNavigate.emit();
  }
}
