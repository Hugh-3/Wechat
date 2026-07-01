#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DEPLOY_DIR="$PROJECT_DIR/deploy"
LOG_DIR="$HOME/.linliquan/logs"
DEPLOY_LOG="$LOG_DIR/deploy-$(date +%Y%m%d-%H%M%S).log"

mkdir -p "$LOG_DIR"
: > "$DEPLOY_LOG"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC}  $*" | tee -a "$DEPLOY_LOG" > /dev/null; echo -e "${BLUE}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*" | tee -a "$DEPLOY_LOG" > /dev/null; echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*" | tee -a "$DEPLOY_LOG" > /dev/null; echo -e "${RED}[ERROR]${NC} $*"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $*" | tee -a "$DEPLOY_LOG" > /dev/null; echo -e "${GREEN}[OK]${NC}    $*"; }

check_docker() {
    log_info "检查 Docker 环境..."
    if ! command -v docker &> /dev/null; then
        log_error "未检测到 Docker，请先安装 Docker: https://docs.docker.com/engine/install/"
        exit 1
    fi
    log_ok "Docker 版本: $(docker --version | awk '{print $3}')"

    if docker compose version &> /dev/null; then
        log_ok "Docker Compose 版本: $(docker compose version | awk '{print $4}')"
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose &> /dev/null; then
        log_ok "docker-compose 版本: $(docker-compose --version | awk '{print $3}')"
        COMPOSE_CMD="docker-compose"
    else
        log_error "未检测到 Docker Compose，请先安装"
        exit 1
    fi
}

check_env_file() {
    log_info "检查环境配置文件..."
    if [ ! -f "$DEPLOY_DIR/.env" ]; then
        log_warn ".env 文件不存在，从 .env.example 生成默认配置"
        cp "$DEPLOY_DIR/.env.example" "$DEPLOY_DIR/.env"

        if command -v openssl &> /dev/null; then
            JWT_SECRET=$(openssl rand -base64 32 | tr -d '\n')
            DB_PASSWORD=$(openssl rand -base64 16 | tr -d '\n/=+')
            REDIS_PASSWORD=$(openssl rand -base64 16 | tr -d '\n/=+')
        elif command -v python3 &> /dev/null; then
            JWT_SECRET=$(python3 -c "import secrets; print(secrets.token_urlsafe(32))")
            DB_PASSWORD=$(python3 -c "import secrets; print(secrets.token_urlsafe(16))")
            REDIS_PASSWORD=$(python3 -c "import secrets; print(secrets.token_urlsafe(16))")
        else
            JWT_SECRET="linliquan-test-jwt-secret-$(date +%s)"
            DB_PASSWORD="linliquan_test_db_$(date +%s)"
            REDIS_PASSWORD="linliquan_test_redis_$(date +%s)"
        fi

        sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$JWT_SECRET|" "$DEPLOY_DIR/.env"
        sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=$DB_PASSWORD|" "$DEPLOY_DIR/.env"
        sed -i "s|^REDIS_PASSWORD=.*|REDIS_PASSWORD=$REDIS_PASSWORD|" "$DEPLOY_DIR/.env"

        log_ok "已自动生成随机密钥，配置文件: $DEPLOY_DIR/.env"
    else
        log_ok ".env 配置文件已存在"
    fi
}

create_directories() {
    log_info "创建必要目录..."
    mkdir -p "$DEPLOY_DIR/uploads"
    mkdir -p "$DEPLOY_DIR/nginx/ssl"
    log_ok "目录创建完成"
}

pull_images() {
    log_info "拉取 Docker 镜像（可能需要几分钟）..."
    cd "$DEPLOY_DIR"
    if $COMPOSE_CMD pull postgres redis nginx 2>&1 | tee -a "$DEPLOY_LOG"; then
        log_ok "基础镜像拉取完成"
    else
        log_warn "镜像拉取遇到问题，将在启动时自动构建/拉取"
    fi
}

start_services() {
    log_info "启动所有服务..."
    cd "$DEPLOY_DIR"

    log_info "  正在构建并启动后端服务..."
    if $COMPOSE_CMD up -d --build 2>&1 | tee -a "$DEPLOY_LOG"; then
        log_ok "服务启动命令已执行"
    else
        log_error "服务启动失败，请检查日志: $DEPLOY_LOG"
        exit 1
    fi
}

wait_for_health() {
    local max_wait=180
    local interval=10
    local waited=0

    log_info "等待服务健康检查（最多 ${max_wait} 秒）..."

    while [ $waited -lt $max_wait ]; do
        cd "$DEPLOY_DIR"
        local postgres_ok=0 redis_ok=0 backend_ok=0 nginx_ok=0

        if $COMPOSE_CMD ps postgres 2>/dev/null | grep -q "healthy"; then
            postgres_ok=1
        fi
        if $COMPOSE_CMD ps redis 2>/dev/null | grep -q "healthy"; then
            redis_ok=1
        fi
        if $COMPOSE_CMD ps backend 2>/dev/null | grep -q "healthy"; then
            backend_ok=1
        fi
        if $COMPOSE_CMD ps nginx 2>/dev/null | grep -q "Up"; then
            nginx_ok=1
        fi

        local all_ok=$((postgres_ok && redis_ok && backend_ok && nginx_ok))

        if [ $all_ok -eq 1 ]; then
            log_ok "所有服务健康检查通过"
            return 0
        fi

        printf "  等待中 (%03d/%ds)  Postgres:%s Redis:%s Backend:%s Nginx:%s\r" \
            "$waited" "$max_wait" \
            "$([ $postgres_ok -eq 1 ] && echo '✓' || echo ' ')" \
            "$([ $redis_ok -eq 1 ] && echo '✓' || echo ' ')" \
            "$([ $backend_ok -eq 1 ] && echo '✓' || echo ' ')" \
            "$([ $nginx_ok -eq 1 ] && echo '✓' || echo ' ')"
        sleep "$interval"
        waited=$((waited + interval))
    done

    echo ""
    log_warn "等待超时，请手动检查服务状态"
    return 1
}

verify_api() {
    log_info "验证 API 接口..."

    local base_url="http://localhost:8080/api"

    if command -v curl &> /dev/null; then
        sleep 5

        log_info "  测试认证状态接口..."
        if curl -s -f "$base_url/v1/auth/status" &> /dev/null; then
            log_ok "认证状态接口正常"
        else
            log_warn "认证状态接口响应异常，可能仍在初始化"
        fi
    else
        log_warn "未安装 curl，跳过接口验证"
    fi
}

show_summary() {
    echo ""
    echo "=============================================="
    echo -e "  ${GREEN}邻里圈测试环境部署完成！${NC}"
    echo "=============================================="
    echo ""
    echo "  服务地址："
    echo "    API 地址:  http://localhost:8080/api"
    echo "    Nginx 地址: http://localhost:80"
    echo "    PostgreSQL: localhost:5432 (数据库: linliquan)"
    echo "    Redis:      localhost:6379"
    echo ""
    echo "  配置文件: $DEPLOY_DIR/.env"
    echo "  部署日志: $DEPLOY_LOG"
    echo ""
    echo "  常用命令："
    echo "    查看状态:  cd $DEPLOY_DIR && docker compose ps"
    echo "    查看日志:  cd $DEPLOY_DIR && docker compose logs -f backend"
    echo "    停止服务:  cd $DEPLOY_DIR && docker compose down"
    echo "    重启服务:  cd $DEPLOY_DIR && docker compose restart"
    echo ""
    echo "=============================================="
}

main() {
    echo ""
    echo "=============================================="
    echo "  邻里圈 - 测试环境一键部署脚本"
    echo "=============================================="
    echo ""

    check_docker
    check_env_file
    create_directories
    pull_images
    start_services
    wait_for_health || true
    verify_api || true
    show_summary
}

main "$@"
