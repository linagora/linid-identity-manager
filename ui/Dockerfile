FROM node:22-alpine AS builder
RUN corepack enable && corepack prepare pnpm@latest --activate
WORKDIR /app
COPY . .
ENV CI=true
RUN rm -rf node_modules
RUN pnpm install --frozen-lockfile
RUN pnpm build

FROM nginx:stable-alpine
RUN rm -rf /usr/share/nginx/html/*
COPY --from=builder /app/dist/spa /usr/share/nginx/html
COPY docker-entrypoint.sh /docker-entrypoint.sh
EXPOSE 443
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
