#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$SCRIPT_DIR"

if [ -f "$DEPLOY_DIR/.env" ]; then
    source "$DEPLOY_DIR/.env"
fi

COMPOSE_CMD="docker compose"
if ! docker compose version &> /dev/null; then
    COMPOSE_CMD="docker-compose"
fi

cd "$DEPLOY_DIR"

case "${1:-help}" in
    start)
        echo "启动服务..."
        $COMPOSE_CMD up -d
        echo "服务已启动"
        ;;
    stop)
        echo "停止服务..."
        $COMPOSE_CMD down
        echo "服务已停止"
        ;;
    restart)
        echo "重启服务..."
        $COMPOSE_CMD restart
        echo "服务已重启"
        ;;
    status)
        echo "服务状态："
        $COMPOSE_CMD ps
        ;;
    logs)
        SERVICE="${2:-backend}"
        echo "查看 $SERVICE 日志 (Ctrl+C 退出)..."
        $COMPOSE_CMD logs -f "$SERVICE"
        ;;
    rebuild)
        echo "重新构建并启动后端..."
        $COMPOSE_CMD up -d --build backend
        echo "后端已重新构建"
        ;;
    clean)
        echo "停止并清理所有数据..."
        read -p "此操作将删除所有数据，确定吗？(y/N): " confirm
        if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
            $COMPOSE_CMD down -v
            echo "已清理所有数据"
        else
            echo "已取消"
        fi
        ;;
    test)
        echo "运行接口冒烟测试..."
        BASE_URL="http://localhost:${BACKEND_PORT:-8080}/api"
        echo "测试地址: $BASE_URL"
        echo ""
        echo "  1. 认证状态接口..."
        curl -s "$BASE_URL/v1/auth/status" | head -c 200
        echo ""
        echo "  2. 帖子列表接口..."
        curl -s "$BASE_URL/v1/posts?page=0&size=5" | head -c 300
        echo ""
        echo "测试完成"
        ;;
    help|*)
        echo "用法: ./manage.sh <命令> [参数]"
        echo ""
        echo "命令列表："
        echo "  start      启动所有服务"
        echo "  stop       停止所有服务"
        echo "  restart    重启所有服务"
        echo "  status     查看服务状态"
        echo "  logs [服务] 查看服务日志 (默认: backend)"
        echo "  rebuild    重新构建后端镜像并启动"
        echo "  test       运行接口冒烟测试"
        echo "  clean      停止并清理所有数据（含数据库）"
        echo "  help       显示此帮助"
        echo ""
        echo "服务名称: postgres, redis, backend, nginx"
        ;;
esac
