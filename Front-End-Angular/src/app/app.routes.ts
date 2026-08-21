import { Routes } from '@angular/router';
import { adminGuard } from './guards/admin.guard';
import { LayoutPublico } from './layouts/layout-publico/layout-publico';
import { Artigo } from './pages/artigo/artigo';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';
import { NaoEncontrada } from './pages/nao-encontrada/nao-encontrada';
import { Painel } from './pages/painel/painel';
import { Sobre } from './pages/sobre/sobre';
import { Tag } from './pages/tag/tag';

export const routes: Routes = [
  // O blog, com cabecalho e rodape. O painel fica fora desta moldura de
  // proposito: a barra do blog nao faz sentido na tela de login.
  {
    path: '',
    component: LayoutPublico,
    children: [
      { path: '', component: Home, title: 'GitGud — reviews de jogos' },
      { path: 'artigo/:slug', component: Artigo },
      { path: 'tag/:slug', component: Tag },
      { path: 'sobre', component: Sobre },
    ],
  },

  { path: 'admin/login', component: Login, title: 'Entrar — GitGud' },
  { path: 'admin', component: Painel, canActivate: [adminGuard], title: 'Painel — GitGud' },

  { path: '**', component: NaoEncontrada, title: 'Página não encontrada — GitGud' },
];
