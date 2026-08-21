import { Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Tag } from '../../types/artigo.type';

/** Trilha de tags para navegar entre assuntos. */
@Component({
  selector: 'app-lista-de-tags',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './lista-de-tags.html',
  styleUrl: './lista-de-tags.scss',
})
export class ListaDeTags {
  readonly tags = input.required<Tag[]>();
}
