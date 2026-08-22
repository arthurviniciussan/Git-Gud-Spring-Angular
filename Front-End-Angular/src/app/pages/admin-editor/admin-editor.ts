import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { EditorDeMarkdown } from '../../components/editor-de-markdown/editor-de-markdown';
import { AdminArtigoService } from '../../services/admin-artigo.service';
import { RascunhoLocalService } from '../../services/rascunho-local.service';
import { ArtigoDoPainel, EnvioDeArtigo } from '../../types/artigo.type';

/** Tela de escrever e editar. Serve para artigo novo e para artigo existente. */
@Component({
  selector: 'app-admin-editor',
  imports: [ReactiveFormsModule, RouterLink, EditorDeMarkdown],
  templateUrl: './admin-editor.html',
  styleUrl: './admin-editor.scss',
})
export class AdminEditor {
  private readonly admin = inject(AdminArtigoService);
  private readonly rascunhoLocal = inject(RascunhoLocalService);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastr = inject(ToastrService);
  private readonly fb = inject(FormBuilder);

  readonly id = signal<string | null>(this.rota.snapshot.paramMap.get('id'));
  readonly carregando = signal(false);
  readonly salvando = signal(false);
  readonly salvo = signal(true);
  readonly artigo = signal<ArtigoDoPainel | null>(null);
  readonly rascunhoRecuperavel = signal<EnvioDeArtigo | null>(null);

  readonly editando = computed(() => this.id() !== null);
  readonly publicado = computed(() => this.artigo()?.status === 'PUBLISHED');

  readonly formulario = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(160)]],
    summary: ['', [Validators.required, Validators.maxLength(300)]],
    contentMarkdown: ['', Validators.required],
    coverImageUrl: [''],
    game: [''],
    score: [null as number | null],
    tags: [''],
  });

  constructor() {
    this.formulario.valueChanges.subscribe(() => {
      // Só edição de gente conta. Carregar um artigo também dispara valueChanges,
      // e sem esta guarda o editor gravaria um rascunho na abertura e logo em
      // seguida ofereceria de volta o rascunho que ele mesmo acabou de escrever.
      if (!this.formulario.dirty) {
        return;
      }
      this.salvo.set(false);
      this.rascunhoLocal.guardar(this.chaveDoRascunho(), this.paraEnvio());
    });

    if (this.editando()) {
      this.carregar(this.id()!);
    } else {
      this.oferecerRascunho();
    }
  }

  /** Usado pelo guard de saída: bloqueia fechar a aba com texto não salvo. */
  temAlteracaoNaoSalva(): boolean {
    return !this.salvo() && this.formulario.dirty;
  }

  textoMudou(markdown: string): void {
    // markAsDirty ANTES do setValue: o valueChanges dispara dentro do setValue,
    // e é ele quem decide se guarda o rascunho.
    this.formulario.controls.contentMarkdown.markAsDirty();
    this.formulario.controls.contentMarkdown.setValue(markdown);
  }

  restaurarRascunho(): void {
    const rascunho = this.rascunhoRecuperavel();
    if (rascunho) {
      // O formulário usa string vazia onde o envio à API usa null.
      this.formulario.patchValue({
        title: rascunho.title,
        summary: rascunho.summary,
        contentMarkdown: rascunho.contentMarkdown,
        coverImageUrl: rascunho.coverImageUrl ?? '',
        game: rascunho.game ?? '',
        score: rascunho.score,
        tags: rascunho.tags.join(', '),
      });
      this.rascunhoRecuperavel.set(null);
      this.toastr.success('Rascunho recuperado.');
    }
  }

  descartarRascunho(): void {
    this.rascunhoLocal.descartar(this.chaveDoRascunho());
    this.rascunhoRecuperavel.set(null);
  }

  salvar(publicarDepois = false): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.toastr.error('Título, resumo e texto são obrigatórios.');
      return;
    }

    this.salvando.set(true);
    const envio = this.paraEnvio();
    const id = this.id();

    const requisicao = id ? this.admin.atualizar(id, envio) : this.admin.criar(envio);

    requisicao.subscribe({
      next: (salvo) => {
        this.aposSalvar(salvo);
        if (publicarDepois && salvo.status === 'DRAFT') {
          this.publicar(salvo.id);
        } else {
          this.salvando.set(false);
          this.toastr.success('Artigo salvo.');
        }
      },
      error: (falha: { error?: { message?: string } }) => {
        this.salvando.set(false);
        this.toastr.error(falha.error?.message ?? 'Não foi possível salvar.');
      },
    });
  }

  private publicar(id: string): void {
    this.admin.publicar(id).subscribe({
      next: (publicado) => {
        this.artigo.set(publicado);
        this.salvando.set(false);
        this.toastr.success('Artigo publicado.');
      },
      error: () => {
        this.salvando.set(false);
        this.toastr.error('Salvou, mas não foi possível publicar.');
      },
    });
  }

  private aposSalvar(salvo: ArtigoDoPainel): void {
    this.artigo.set(salvo);
    this.salvo.set(true);
    this.formulario.markAsPristine();
    this.rascunhoLocal.descartar(this.chaveDoRascunho());

    if (!this.editando()) {
      // Passa a editar o artigo recém-criado, sem recarregar a tela.
      this.id.set(salvo.id);
      void this.router.navigate(['/admin/artigos', salvo.id, 'editar'], { replaceUrl: true });
    }
  }

  private carregar(id: string): void {
    this.carregando.set(true);

    this.admin.porId(id).subscribe({
      next: (artigo) => {
        this.artigo.set(artigo);
        this.formulario.patchValue({
          title: artigo.title,
          summary: artigo.summary,
          contentMarkdown: artigo.contentMarkdown,
          coverImageUrl: artigo.coverImageUrl ?? '',
          game: artigo.game ?? '',
          score: artigo.score,
          tags: artigo.tags.map((tag) => tag.name).join(', '),
        });
        this.formulario.markAsPristine();
        this.salvo.set(true);
        this.carregando.set(false);
        this.oferecerRascunho();
      },
      error: () => {
        this.carregando.set(false);
        this.toastr.error('Artigo não encontrado.');
        void this.router.navigate(['/admin']);
      },
    });
  }

  /** Se houver rascunho local mais novo, oferece de volta em vez de sobrescrever. */
  private oferecerRascunho(): void {
    const guardado = this.rascunhoLocal.recuperar(this.chaveDoRascunho());
    if (guardado && guardado.artigo.contentMarkdown?.trim()) {
      this.rascunhoRecuperavel.set(guardado.artigo);
    }
  }

  private chaveDoRascunho(): string {
    return this.id() ?? 'novo';
  }

  private paraEnvio(): EnvioDeArtigo {
    const valor = this.formulario.getRawValue();

    return {
      title: valor.title,
      summary: valor.summary,
      contentMarkdown: valor.contentMarkdown,
      coverImageUrl: valor.coverImageUrl?.trim() || null,
      game: valor.game?.trim() || null,
      score: valor.score === null || `${valor.score}` === '' ? null : Number(valor.score),
      tags: (valor.tags ?? '')
        .split(',')
        .map((tag) => tag.trim())
        .filter(Boolean),
    };
  }
}
