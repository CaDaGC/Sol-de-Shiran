FROM node:24-bookworm-slim AS build
WORKDIR /build
COPY FrontEnd/package.json FrontEnd/package-lock.json ./
RUN --mount=type=cache,id=sol-shiran-npm,target=/root/.npm npm ci --no-audit --no-fund
COPY FrontEnd/ ./
RUN npm run build

FROM node:24-bookworm-slim
ENV NODE_ENV=production PORT=4000 NG_ALLOWED_HOSTS=localhost,127.0.0.1
WORKDIR /app
COPY --from=build --chown=node:node /build/dist/FrontEnd ./dist/FrontEnd
USER node
EXPOSE 4000
HEALTHCHECK --interval=10s --timeout=5s --start-period=20s --retries=12 CMD node -e "fetch('http://127.0.0.1:4000/login').then(r=>{if(!r.ok)process.exit(1)}).catch(()=>process.exit(1))"
CMD ["node","dist/FrontEnd/server/server.mjs"]
