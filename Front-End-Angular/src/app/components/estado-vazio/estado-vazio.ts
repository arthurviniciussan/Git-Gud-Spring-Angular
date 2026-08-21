import { Component, input } from '@angular/core';

/** Mensagem para lista sem resultado — evita a tela em branco sem explicação. */
@Component({
  selector: 'app-estado-vazio',
  imports: [],
  templateUrl: './estado-vazio.html',
  styleUrl: './estado-vazio.scss',
})
export class EstadoVazio {
  readonly titulo = input.required<string>();
  readonly descricao = input('');
}
