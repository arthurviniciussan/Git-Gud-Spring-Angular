import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CabecalhoDoBlog } from '../../components/cabecalho-do-blog/cabecalho-do-blog';
import { RodapeDoBlog } from '../../components/rodape-do-blog/rodape-do-blog';

/**
 * Moldura das paginas publicas.
 *
 * <p>Existe como rota-pai para que o painel de admin fique fora dela: o
 * cabecalho do blog nao faz sentido na tela de login.
 */
@Component({
  selector: 'app-layout-publico',
  imports: [RouterOutlet, CabecalhoDoBlog, RodapeDoBlog],
  templateUrl: './layout-publico.html',
  styleUrl: './layout-publico.scss',
})
export class LayoutPublico {}
