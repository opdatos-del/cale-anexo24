FROM node:22-alpine AS build

WORKDIR /app

RUN corepack enable \
    && corepack prepare pnpm@12.4.0 --activate

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

COPY angular.json eslint.config.js postcss.config.json tsconfig*.json ./
COPY public ./public
COPY src ./src

RUN pnpm build

FROM nginxinc/nginx-unprivileged:stable-alpine AS runtime

COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=6 \
    CMD wget --quiet --output-document=- http://127.0.0.1:8080/ || exit 1
