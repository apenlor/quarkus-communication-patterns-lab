FROM nginx:1.29.1-alpine

# Copy our custom Nginx configuration to the container
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copy the static web assets (HTML, CSS, JS) to the directory Nginx serves from
COPY static/ /usr/share/nginx/html

EXPOSE 80