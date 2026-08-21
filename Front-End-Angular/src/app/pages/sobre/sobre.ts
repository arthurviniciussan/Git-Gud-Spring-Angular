import { Component, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

/**
 * Quem escreve o blog.
 *
 * <p>Alem do obvio, adianta um requisito da Etapa 5: rede de anuncio costuma
 * exigir uma pagina de identificacao antes de aprovar um site.
 */
@Component({
  selector: 'app-sobre',
  imports: [RouterLink],
  templateUrl: './sobre.html',
  styleUrl: './sobre.scss',
})
export class Sobre {
  constructor() {
    inject(Title).setTitle('Sobre — GitGud');
    inject(Meta).updateTag({
      name: 'description',
      content: 'Quem escreve o GitGud e por que este blog existe.',
    });
  }
}
