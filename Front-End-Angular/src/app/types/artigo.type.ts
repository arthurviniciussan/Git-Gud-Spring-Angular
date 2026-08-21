/** Tag como a API devolve. */
export type Tag = {
  name: string;
  slug: string;
};

/** Item da listagem: sem o corpo do artigo. */
export type ResumoDeArtigo = {
  slug: string;
  title: string;
  summary: string;
  coverImageUrl: string | null;
  game: string | null;
  score: number | null;
  publishedAt: string;
  tags: Tag[];
};

/** Artigo completo. `contentHtml` ja vem sanitizado do servidor. */
export type Artigo = ResumoDeArtigo & {
  contentHtml: string;
};

/** Envelope de paginacao da API. */
export type Pagina<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
};
