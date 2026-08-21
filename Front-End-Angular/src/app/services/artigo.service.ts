import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Artigo, Pagina, ResumoDeArtigo, Tag } from '../types/artigo.type';

/** Filtros aceitos pela listagem. Tag e busca sao mutuamente exclusivos na API. */
export type ConsultaDeArtigos = {
  pagina?: number;
  tamanho?: number;
  tag?: string;
  busca?: string;
};

/**
 * Leitura publica do blog.
 *
 * <p>Separado do que o painel usa: estas chamadas nao dependem de token, e e o
 * interceptor que decide anexar o Authorization quando houver sessao.
 */
@Injectable({ providedIn: 'root' })
export class ArtigoService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  listar(consulta: ConsultaDeArtigos = {}): Observable<Pagina<ResumoDeArtigo>> {
    let parametros = new HttpParams()
      .set('page', consulta.pagina ?? 0)
      .set('size', consulta.tamanho ?? 9);

    if (consulta.tag) {
      parametros = parametros.set('tag', consulta.tag);
    }
    if (consulta.busca) {
      parametros = parametros.set('q', consulta.busca);
    }

    return this.http.get<Pagina<ResumoDeArtigo>>(`${this.url}/articles`, { params: parametros });
  }

  porSlug(slug: string): Observable<Artigo> {
    return this.http.get<Artigo>(`${this.url}/articles/${slug}`);
  }

  tags(): Observable<Tag[]> {
    return this.http.get<Tag[]>(`${this.url}/tags`);
  }
}
