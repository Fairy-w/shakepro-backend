# ShakePro Admin Web

## Local Setup

1. Copy `.env.example` to `.env.local`.
2. Keep the default values if the backend runs at `http://localhost:8080`.
3. Start the frontend:

```bash
npm run dev
```

The default local configuration is:

```env
VITE_API_BASE_URL=/api
VITE_API_PROXY_TARGET=http://localhost:8080
```

With this setup, browser requests go to `/api`, and the Vite dev server proxies them to the Spring Boot backend.

## Environment Variables

- `VITE_API_BASE_URL`: axios request base URL. Use `/api` for local development with proxy, or a full backend URL in deployed environments.
- `VITE_API_PROXY_TARGET`: backend address used only by the Vite dev server proxy.

## Examples

Local development:

```env
VITE_API_BASE_URL=/api
VITE_API_PROXY_TARGET=http://localhost:8080
```

Remote backend during local frontend debugging:

```env
VITE_API_BASE_URL=/api
VITE_API_PROXY_TARGET=http://192.168.1.20:8080
```

Static deployment without Vite proxy:

```env
VITE_API_BASE_URL=http://your-backend-host:8080/api
```
