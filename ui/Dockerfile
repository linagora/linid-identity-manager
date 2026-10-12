FROM node:22-alpine AS builder
RUN corepack enable && corepack prepare pnpm@latest --activate
WORKDIR /app
COPY . .
ENV CI=true
RUN rm -rf node_modules dist server/node_modules server/dist
RUN pnpm install --frozen-lockfile
RUN pnpm build
# Build the server, then extract it with its production dependencies only (no SPA nor dev dependencies)
RUN pnpm --filter @linagora/linid-identity-manager-ui-server build
RUN pnpm --filter @linagora/linid-identity-manager-ui-server --prod --config.inject-workspace-packages=true deploy /deploy/server

FROM node:22-alpine
RUN apk upgrade --no-cache
WORKDIR /app
COPY --from=builder /deploy/server ./server
COPY --from=builder /app/dist/spa ./spa
# server-config.json is mounted at /app/server-config.json (generated per environment from server-config.template.json)
EXPOSE 443
CMD ["node", "server/dist/main.js"]
