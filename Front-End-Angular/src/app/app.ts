import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TemaService } from './services/tema.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  // Injetado so para instanciar: o construtor do servico aplica o tema guardado
  // antes da primeira pintura, evitando o flash de tema claro.
  private readonly tema = inject(TemaService);
}
