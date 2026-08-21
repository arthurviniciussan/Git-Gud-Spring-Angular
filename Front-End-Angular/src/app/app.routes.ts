import { Routes } from '@angular/router';
import { adminGuard } from './guards/admin.guard';
import { Login } from './pages/login/login';
import { NaoEncontrada } from './pages/nao-encontrada/nao-encontrada';
import { Painel } from './pages/painel/painel';

export const routes: Routes = [
  // Enquanto o blog publico nao existe, a raiz leva ao login. Na Etapa 3 esta
  // rota passa a ser a home com a lista de artigos.
  { path: '', redirectTo: 'admin/login', pathMatch: 'full' },

  { path: 'admin/login', component: Login },
  { path: 'admin', component: Painel, canActivate: [adminGuard] },

  // Sem esta rota, qualquer endereco errado deixava a pagina em branco.
  { path: '**', component: NaoEncontrada },
];
