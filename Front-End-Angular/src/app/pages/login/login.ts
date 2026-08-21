import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { DefaultLoginLayout } from '../../components/default-login-layout/default-login-layout';
import { PrimaryInput } from '../../components/primary-input/primary-input';
import { AuthService } from '../../services/auth.service';
import { ErroDaApi } from '../../types/auth.type';

@Component({
  selector: 'app-login',
  imports: [DefaultLoginLayout, ReactiveFormsModule, PrimaryInput],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly toastr = inject(ToastrService);

  readonly enviando = signal(false);
  readonly erro = signal<string | null>(null);

  readonly loginForm = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required, Validators.minLength(8)]),
  });

  submit(): void {
    if (this.loginForm.invalid || this.enviando()) {
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);

    const { email, password } = this.loginForm.getRawValue();

    this.auth.login(email ?? '', password ?? '').subscribe({
      next: () => {
        this.enviando.set(false);
        this.toastr.success('Bem-vindo de volta.');
        // A navegacao ficava FORA do subscribe: entrava no painel mesmo com a
        // senha errada. Agora so acontece quando a API confirma o login.
        void this.router.navigate(['/admin']);
      },
      error: (falha: HttpErrorResponse) => {
        this.enviando.set(false);
        const mensagem = this.mensagemDe(falha);
        this.erro.set(mensagem);
        this.toastr.error(mensagem);
      },
    });
  }

  /** Usa a mensagem da API — inclusive a de "tentativas demais", que o usuario precisa ver. */
  private mensagemDe(falha: HttpErrorResponse): string {
    const corpo = falha.error as ErroDaApi | null;

    if (corpo?.message) {
      return corpo.message;
    }
    if (falha.status === 0) {
      return 'Não foi possível falar com o servidor.';
    }
    return 'Não foi possível entrar. Tente novamente.';
  }
}
