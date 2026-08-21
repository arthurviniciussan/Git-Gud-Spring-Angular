import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-rodape-do-blog',
  imports: [RouterLink],
  templateUrl: './rodape-do-blog.html',
  styleUrl: './rodape-do-blog.scss',
})
export class RodapeDoBlog {
  readonly ano = new Date().getFullYear();
}
