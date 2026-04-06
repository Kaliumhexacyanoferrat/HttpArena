FROM trueasync/php-true-async:latest-frankenphp

WORKDIR /app

COPY worker.php /app/worker.php
COPY Caddyfile /etc/caddy/Caddyfile

# OPcache JIT tuning
RUN echo '\
opcache.enable=1\n\
opcache.enable_cli=1\n\
opcache.jit=1255\n\
opcache.jit_buffer_size=128M\n\
opcache.memory_consumption=256\n\
opcache.max_accelerated_files=10000\n\
opcache.validate_timestamps=0\n\
memory_limit=512M\n\
' >> /etc/php.d/99-benchmark.ini

ENV GODEBUG=cgocheck=0

EXPOSE 8080
EXPOSE 8443

CMD ["frankenphp", "run", "--config", "/etc/caddy/Caddyfile", "--adapter", "caddyfile"]
