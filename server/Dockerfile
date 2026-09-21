FROM node:22-alpine
WORKDIR /app
COPY package.json ./
COPY src ./src
ENV NODE_ENV=production
EXPOSE 8787
CMD ["node", "src/server.mjs"]
