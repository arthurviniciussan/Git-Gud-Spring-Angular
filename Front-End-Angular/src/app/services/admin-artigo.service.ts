import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ArtigoDoPainel,
  EnvioDeArtigo,
  ImagemEnviada,
  Pagina,
} from '../types/artigo.type';

/**
 * O que o painel usa. Separado do ArtigoService publico de proposito: estas
 * chamadas exigem token, e o interceptor o anexa.
 */
@Injectable({ providedIn: 'root' })
export class AdminArtigoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin/articles`;

  listar(pagina = 0, tamanho = 20): Observable<Pagina<ArtigoDoPainel>> {
    const parametros = new HttpParams().set('page', pagina).set('size', tamanho);
    return this.http.get<Pagina<ArtigoDoPainel>>(this.url, { params: parametros });
  }

  porId(id: string): Observable<ArtigoDoPainel> {
    return this.http.get<ArtigoDoPainel>(`${this.url}/${id}`);
  }

  criar(artigo: EnvioDeArtigo): Observable<ArtigoDoPainel> {
    return this.http.post<ArtigoDoPainel>(this.url, artigo);
  }

  atualizar(id: string, artigo: EnvioDeArtigo): Observable<ArtigoDoPainel> {
    return this.http.put<ArtigoDoPainel>(`${this.url}/${id}`, artigo);
  }

  publicar(id: string): Observable<ArtigoDoPainel> {
    return this.http.patch<ArtigoDoPainel>(`${this.url}/${id}/publish`, {});
  }

  despublicar(id: string): Observable<ArtigoDoPainel> {
    return this.http.patch<ArtigoDoPainel>(`${this.url}/${id}/unpublish`, {});
  }

  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  /** Preview: a mesma conversão que o artigo salvo recebe, feita no servidor. */
  preview(markdown: string): Observable<string> {
    return this.http
      .post<{ html: string }>(`${this.url}/preview`, { markdown })
      .pipe(map((resposta) => resposta.html));
  }

  enviarImagem(arquivo: File): Observable<ImagemEnviada> {
    const corpo = new FormData();
    corpo.append('arquivo', arquivo);

    return this.http.post<ImagemEnviada>(`${environment.apiUrl}/admin/images`, corpo);
  }
}
