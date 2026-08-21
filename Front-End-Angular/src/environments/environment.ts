/**
 * Valores de producao. O build de desenvolvimento troca este arquivo pelo
 * environment.development.ts (fileReplacements no angular.json).
 *
 * Existe para tirar o endereco da API de dentro do codigo: antes o
 * `http://localhost:8080` estava fixo no servico de login, o que simplesmente
 * nao funciona fora da maquina do desenvolvedor.
 */
export const environment = {
  producao: true,
  apiUrl: '/api',
};
