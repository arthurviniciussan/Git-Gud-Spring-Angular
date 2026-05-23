FROM node:22-alpine

WORKDIR /app

# Copia os arquivos de dependências primeiro (aproveita o cache do Docker)
COPY package*.json ./

# Instala as dependências do projeto
RUN npm install

# Copia o restante dos arquivos do código fonte
COPY . .

# Expõe a porta padrão do Angular
EXPOSE 4200

# Comando para rodar a aplicação escutando todas as interfaces de rede
CMD ["npm", "start", "--", "--host", "0.0.0.0", "--poll", "2000"]