/** Resposta de POST /api/auth/login. */
export type RespostaDeLogin = {
  name: string;
  email: string;
  role: string;
  token: string;
};

/** Resposta de GET /api/auth/session — quem esta logado, sem token novo. */
export type Sessao = {
  name: string;
  email: string;
  role: string;
};

/** Corpo unico de erro da API. */
export type ErroDaApi = {
  message: string;
  timestamp: string;
};
