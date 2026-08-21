import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Moldura das telas de entrada: logo, titulo, conteudo projetado e os botoes.
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
  logoImage = 'assets/log-Git-Gud!.png';
  bonfireImage = '/assets/main-bonfire-image-removebg.png';

  @Input() title = '';
  @Input() primaryBtnText = '';
  /** Vazio esconde o botao secundario e o separador "or". */
  @Input() secondaryBtnText = '';
  @Input() disablePrimaryBtn = true;

  @Output('submit') onSubmit = new EventEmitter<void>();
  @Output('navigate') onNavigate = new EventEmitter<void>();

  submit(): void {
    this.onSubmit.emit();
  }

  navigate(): void {
    this.onNavigate.emit();
  }
}
