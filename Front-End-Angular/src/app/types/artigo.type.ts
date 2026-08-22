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

/** Artigo como o painel precisa ver: com id, markdown e status. */
export type ArtigoDoPainel = {
  id: string;
  slug: string;
  title: string;
  summary: string;
  contentMarkdown: string;
  coverImageUrl: string | null;
  game: string | null;
  score: number | null;
  status: 'DRAFT' | 'PUBLISHED';
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
  tags: Tag[];
};

/** Corpo de criação e edição. */
export type EnvioDeArtigo = {
  title: string;
  summary: string;
  contentMarkdown: string;
  coverImageUrl: string | null;
  game: string | null;
  score: number | null;
  tags: string[];
};

/** Resposta do upload de imagem. */
export type ImagemEnviada = {
  url: string;
  largura: number;
  altura: number;
  bytes: number;
};
