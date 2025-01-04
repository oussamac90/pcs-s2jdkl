# Stage 1: Build Angular application
FROM node:18-alpine AS build

# Set working directory
WORKDIR /app

# Create non-root user for build process
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy package files for dependency installation
COPY --chown=appuser:appgroup package*.json ./

# Install dependencies using clean install for reproducible builds
RUN npm ci --no-audit --no-fund

# Copy source code and configuration files
COPY --chown=appuser:appgroup . .

# Set build environment variables
ARG BUILD_VERSION
ARG API_URL
ENV NODE_ENV=production
ENV BUILD_VERSION=${BUILD_VERSION}
ENV API_URL=${API_URL}

# Build production application with optimization flags
RUN npm run build:prod -- \
    --output-path=dist \
    --optimization=true \
    --sourceMap=false \
    --namedChunks=false \
    --extractLicenses=true \
    --vendorChunk=true \
    --buildOptimizer=true \
    --progress=false \
    --statsJson=false

# Stage 2: Production environment
FROM nginx:1.25-alpine

# Create non-root nginx user
RUN addgroup -S nginx && adduser -S nginx -G nginx

# Copy nginx configuration
COPY --chown=nginx:nginx nginx.conf /etc/nginx/nginx.conf

# Remove default nginx configuration
RUN rm -rf /etc/nginx/conf.d/default.conf

# Copy built application from build stage
COPY --from=build --chown=nginx:nginx /app/dist /usr/share/nginx/html

# Set proper permissions
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

# Create required directories with proper permissions
RUN mkdir -p /etc/nginx/ssl && \
    chown -R nginx:nginx /etc/nginx/ssl && \
    chmod 700 /etc/nginx/ssl

# Switch to non-root user
USER nginx

# Expose port 80
EXPOSE 80

# Add healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:80/health || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]