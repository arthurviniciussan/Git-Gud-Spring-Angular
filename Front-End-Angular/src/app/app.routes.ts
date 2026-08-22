import { Routes } from '@angular/router';
import { adminGuard } from './guards/admin.guard';
import { saidaDoEditorGuard } from './guards/saida-do-editor.guard';
import { LayoutAdmin } from './layouts/layout-admin/layout-admin';
import { LayoutPublico } from './layouts/layout-publico/layout-publico';
import { AdminArtigos } from './pages/admin-artigos/admin-artigos';
import { AdminEditor } from './pages/admin-editor/admin-editor';
import { Artigo } from './pages/artigo/artigo';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';
import { NaoEncontrada } from './pages/nao-encontrada/nao-encontrada';
import { Sobre } from './pages/sobre/sobre';
import { Tag } from './pages/tag/tag';

export const routes: Routes = [
  // O blog, com cabecalho e rodape.
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

  // O painel fica fora da moldura do blog e nao e linkado de lugar nenhum:
  // so se chega nele digitando a URL, e so se entra com a senha certa.
  { path: 'admin/login', component: Login, title: 'Entrar — GitGud' },
  {
    path: 'admin',
    component: LayoutAdmin,
    canActivate: [adminGuard],
    children: [
      { path: '', component: AdminArtigos, title: 'Artigos — GitGud' },
      {
        path: 'artigos/novo',
        component: AdminEditor,
        title: 'Escrever artigo — GitGud',
        canDeactivate: [saidaDoEditorGuard],
      },
      {
        path: 'artigos/:id/editar',
        component: AdminEditor,
        title: 'Editar artigo — GitGud',
        canDeactivate: [saidaDoEditorGuard],
      },
    ],
  },

  { path: '**', component: NaoEncontrada, title: 'Página não encontrada — GitGud' },
];
