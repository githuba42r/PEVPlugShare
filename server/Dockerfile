FROM node:20-slim
WORKDIR /app
COPY package*.json ./
RUN npm ci --production
COPY dist/ ./dist/
COPY migrations/ ./migrations/
EXPOSE 8080
CMD ["node", "dist/server.js"]
