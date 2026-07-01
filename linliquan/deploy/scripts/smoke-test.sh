#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

API_BASE="${API_BASE_URL:-http://localhost:8080/api}"
OUTPUT_DIR="$SCRIPT_DIR/test-results"
REPORT_FILE="$OUTPUT_DIR/smoke-test-report-$(date +%Y%m%d-%H%M%S).md"

mkdir -p "$OUTPUT_DIR"

PASS_COUNT=0
FAIL_COUNT=0
TEST_CASES=()

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_pass() {
    PASS_COUNT=$((PASS_COUNT + 1))
    echo -e "  ${GREEN}✓ PASS${NC}  $1"
    TEST_CASES+=("PASS|$1|$2")
}

log_fail() {
    FAIL_COUNT=$((FAIL_COUNT + 1))
    echo -e "  ${RED}✗ FAIL${NC}  $1"
    TEST_CASES+=("FAIL|$1|$2")
}

test_api() {
    local name="$1"
    local method="$2"
    local url="$3"
    local body="${4:-}"
    local auth_header="${5:-}"
    local expected_status="${6:-200}"

    local curl_args=(-s -w "\n%{http_code}" -X "$method" "$API_BASE$url")
    curl_args+=(-H "Content-Type: application/json")
    curl_args+=(-H "X-Test-Request: smoke-test")

    if [ -n "$auth_header" ]; then
        curl_args+=(-H "Authorization: Bearer $auth_header")
    fi

    if [ -n "$body" ]; then
        curl_args+=(-d "$body")
    fi

    local response
    response=$(curl "${curl_args[@]}" 2>&1)
    local http_code=$(echo "$response" | tail -n1)
    local body_content=$(echo "$response" | sed '$d')

    if [ "$http_code" = "$expected_status" ]; then
        log_pass "$name" "HTTP $http_code"
    else
        log_fail "$name" "期望 HTTP $expected_status, 实际 HTTP $http_code, 响应: $(echo "$body_content" | head -c 200)"
    fi
}

test_json_field() {
    local name="$1"
    local method="$2"
    local url="$3"
    local json_path="$4"
    local expected_value="$5"
    local auth_header="${6:-}"

    local curl_args=(-s -X "$method" "$API_BASE$url")
    curl_args+=(-H "Content-Type: application/json")
    if [ -n "$auth_header" ]; then
        curl_args+=(-H "Authorization: Bearer $auth_header")
    fi

    local response
    response=$(curl "${curl_args[@]}" 2>&1)

    local actual_value
    if command -v jq &> /dev/null; then
        actual_value=$(echo "$response" | jq -r "$json_path" 2>/dev/null || echo "N/A")
    else
        actual_value=$(echo "$response" | grep -o "\"$json_path\"[^,}]*" | head -1 | sed 's/.*: *//' | tr -d '"' || echo "N/A")
    fi

    if [ "$actual_value" = "$expected_value" ]; then
        log_pass "$name" "$json_path = $actual_value"
    else
        log_fail "$name" "期望 $json_path=$expected_value, 实际=$actual_value"
    fi
}

echo ""
echo "=============================================="
echo "  邻里圈 - 接口冒烟测试"
echo "=============================================="
echo ""
echo "  API 地址: $API_BASE"
echo "  测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

echo ""
echo -e "${BLUE}【P0】认证模块测试${NC}"
echo ""

test_api "登录接口" "POST" "/v1/auth/login?phone=13800138000" "" "" "200"
test_api "Mock登录-未认证" "POST" "/v1/auth/login-mock?statusType=0" "" "" "200"
test_api "Mock登录-认证中" "POST" "/v1/auth/login-mock?statusType=1" "" "" "200"
test_api "Mock登录-已认证" "POST" "/v1/auth/login-mock?statusType=2" "" "" "200"
test_api "查询认证状态(未登录)" "GET" "/v1/auth/status" "" "" "200"

VERIFIED_TOKEN=""
if command -v jq &> /dev/null; then
    VERIFIED_TOKEN=$(curl -s -X POST "$API_BASE/v1/auth/login-mock?statusType=2" | jq -r '.data.accessToken' 2>/dev/null || echo "")
fi

UNAUTH_TOKEN=""
if command -v jq &> /dev/null; then
    UNAUTH_TOKEN=$(curl -s -X POST "$API_BASE/v1/auth/login-mock?statusType=0" | jq -r '.data.accessToken' 2>/dev/null || echo "")
fi

PENDING_TOKEN=""
if command -v jq &> /dev/null; then
    PENDING_TOKEN=$(curl -s -X POST "$API_BASE/v1/auth/login-mock?statusType=1" | jq -r '.data.accessToken' 2>/dev/null || echo "")
fi

echo ""
echo -e "${BLUE}【P0】帖子模块测试${NC}"
echo ""

test_api "帖子列表查询" "GET" "/v1/posts?page=1&pageSize=10" "" "" "200"
test_api "帖子详情查询" "GET" "/v1/posts/1" "" "" "200"

if [ -n "$VERIFIED_TOKEN" ]; then
    test_api "已认证用户发布帖子" "POST" "/v1/posts" '{"title":"冒烟测试","content":"测试内容","postType":1,"lat":39.9042,"lng":116.4074}' "$VERIFIED_TOKEN" "200"
else
    echo -e "  ${YELLOW}⚠ SKIP${NC}  已认证用户发布帖子 (无有效Token)"
fi

if [ -n "$UNAUTH_TOKEN" ]; then
    test_api "未认证用户发布帖子-应返回403" "POST" "/v1/posts" '{"title":"test","content":"test","postType":1}' "$UNAUTH_TOKEN" "403"
else
    echo -e "  ${YELLOW}⚠ SKIP${NC}  未认证用户发布帖子-应返回403 (无有效Token)"
fi

if [ -n "$PENDING_TOKEN" ]; then
    test_api "认证中用户发布帖子-应返回403" "POST" "/v1/posts" '{"title":"test","content":"test","postType":1}' "$PENDING_TOKEN" "403"
else
    echo -e "  ${YELLOW}⚠ SKIP${NC}  认证中用户发布帖子-应返回403 (无有效Token)"
fi

echo ""
echo -e "${BLUE}【P0】评论模块测试${NC}"
echo ""

test_api "评论列表查询" "GET" "/v1/comments?postId=1&page=1&pageSize=20" "" "" "200"

if [ -n "$VERIFIED_TOKEN" ]; then
    test_api "已认证用户发表评论" "POST" "/v1/comments" '{"postId":1,"content":"冒烟测试评论"}' "$VERIFIED_TOKEN" "200"
else
    echo -e "  ${YELLOW}⚠ SKIP${NC}  已认证用户发表评论 (无有效Token)"
fi

if [ -n "$UNAUTH_TOKEN" ]; then
    test_api "未认证用户发表评论-应返回403" "POST" "/v1/comments" '{"postId":1,"content":"test"}' "$UNAUTH_TOKEN" "403"
else
    echo -e "  ${YELLOW}⚠ SKIP${NC}  未认证用户发表评论-应返回403 (无有效Token)"
fi

echo ""
echo -e "${BLUE}【P0】互助任务模块测试${NC}"
echo ""

test_api "附近互助任务查询" "GET" "/v1/orders/nearby?lat=39.9042&lng=116.4074&radius=5" "" "" "200"

if [ -n "$VERIFIED_TOKEN" ]; then
    test_api "已认证用户发布互助任务" "POST" "/v1/orders" '{"postId":1,"helpType":1,"rewardAmount":10.00,"lat":39.9042,"lng":116.4074}' "$VERIFIED_TOKEN" "200"
else
    echo -e "  ${YELLOW}⚠ SKIP${NC}  已认证用户发布互助任务 (无有效Token)"
fi

if [ -n "$UNAUTH_TOKEN" ]; then
    test_api "未认证用户发布互助任务-应返回403" "POST" "/v1/orders" '{"postId":1,"helpType":1}' "$UNAUTH_TOKEN" "403"
else
    echo -e "  ${YELLOW}⚠ SKIP${NC}  未认证用户发布互助任务-应返回403 (无有效Token)"
fi

echo ""
echo "=============================================="
echo -e "  测试结果: ${GREEN}$PASS_COUNT 通过${NC}, ${RED}$FAIL_COUNT 失败${NC}"
echo "=============================================="
echo ""

cat > "$REPORT_FILE" << EOF
# 邻里圈 - 接口冒烟测试报告

**测试时间:** $(date '+%Y-%m-%d %H:%M:%S')
**API 地址:** $API_BASE
**测试结果:** ✅ $PASS_COUNT 通过, ❌ $FAIL_COUNT 失败

## 测试详情

| 状态 | 测试用例 | 详情 |
|------|---------|------|
EOF

for tc in "${TEST_CASES[@]}"; do
    IFS='|' read -r status name detail <<< "$tc"
    local status_icon=""
    if [ "$status" = "PASS" ]; then
        status_icon="✅"
    else
        status_icon="❌"
    fi
    echo "| $status_icon | $name | $detail |" >> "$REPORT_FILE"
done

cat >> "$REPORT_FILE" << EOF

## 统计

- 通过: $PASS_COUNT
- 失败: $FAIL_COUNT
- 总计: $((PASS_COUNT + FAIL_COUNT))

---

*报告由冒烟测试脚本自动生成*
EOF

echo "  报告已生成: $REPORT_FILE"
echo ""

if [ $FAIL_COUNT -gt 0 ]; then
    exit 1
fi

exit 0
