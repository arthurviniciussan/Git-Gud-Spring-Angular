import { CanDeactivateFn } from '@angular/router';

/** Componente que sabe dizer se tem coisa não salva. */
export type PodeTerAlteracaoNaoSalva = {
  temAlteracaoNaoSalva(): boolean;
};

/**
 * Avisa antes de sair do editor com texto não salvo.
 *
 * <p>Cobre a navegação dentro do site. Fechar a aba é outro caminho e depende
 * do beforeunload, registrado no proprio editor.
 */
export const saidaDoEditorGuard: CanDeactivateFn<PodeTerAlteracaoNaoSalva> = (componente) => {
  if (!componente.temAlteracaoNaoSalva()) {
    return true;
  }
  return confirm('Você tem alterações não salvas. Sair mesmo assim?');
};
