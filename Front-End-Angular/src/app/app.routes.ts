import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Signup } from './pages/signup/signup';
import { User } from './pages/user/user';
import { AuthGuard } from './services/auth-guard';

export const routes: Routes = [
  {
    path: "login",
    component: Login
  },

    {
    path: "signup",
    component: Signup
  },

  {
    path: "user",
    component: User,
    canActivate: [AuthGuard]
  }
];
