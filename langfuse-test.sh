#!/bin/bash

# =================================
# LangFuse LOCAL Instance Connection Test
# =================================

# Using your actual LangFuse credentials from the screenshot
LANGFUSE_HOST="http://localhost:3000"
LANGFUSE_SECRET_KEY="sk-lf-fe50f236-ba0a-4a35-9052-2fe1561db7bc"
LANGFUSE_PUBLIC_KEY="pk-lf-0ed2f470-5f1b-4b0b-b16b-b75c3cacb750"

echo "🔍 Testing LOCAL LangFuse API Connection"
echo "========================================"
echo "Host: $LANGFUSE_HOST"
echo "Secret Key: ${LANGFUSE_SECRET_KEY:0:15}..."
echo "Public Key: ${LANGFUSE_PUBLIC_KEY:0:15}..."
echo ""

# =================================
# 1. Test LangFuse Local Health
# =================================
echo "1️⃣ Testing Local LangFuse Health..."
curl -X GET "$LANGFUSE_HOST/api/public/health" \
  -H "Accept: application/json" \
  -w "\nStatus: %{http_code}\nTime: %{time_total}s\n"

echo ""
echo "============================================================"

# =================================
# 2. Test with Basic Auth (Local LangFuse Style)
# =================================
echo ""
echo "2️⃣ Testing Basic Authentication..."
curl -X GET "$LANGFUSE_HOST/api/public/projects" \
  -u "$LANGFUSE_PUBLIC_KEY:$LANGFUSE_SECRET_KEY" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -w "\nStatus: %{http_code}\nTime: %{time_total}s\n"

echo ""
echo "============================================================"

# =================================
# 3. Test Creating Trace with Basic Auth
# =================================
echo ""
echo "3️⃣ Testing Trace Creation with Basic Auth..."

TRACE_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')

curl -X POST "$LANGFUSE_HOST/api/public/traces" \
  -u "$LANGFUSE_PUBLIC_KEY:$LANGFUSE_SECRET_KEY" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d "{
    \"id\": \"$TRACE_ID\",
    \"name\": \"local-test-trace\",
    \"userId\": \"curl-local-user\",
    \"sessionId\": \"curl-local-session\",
    \"input\": \"Test message for local LangFuse\",
    \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\"
  }" \
  -w "\nStatus: %{http_code}\nTime: %{time_total}s\n" | jq '.'

echo ""
echo "============================================================"

# =================================
# 4. Test Alternative Auth Header Format
# =================================
echo ""
echo "4️⃣ Testing Alternative Auth Format..."

curl -X POST "$LANGFUSE_HOST/api/public/traces" \
  -H "Authorization: Basic $(echo -n "$LANGFUSE_PUBLIC_KEY:$LANGFUSE_SECRET_KEY" | base64)" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d "{
    \"id\": \"$(uuidgen | tr '[:upper:]' '[:lower:]')\",
    \"name\": \"basic-auth-test\",
    \"userId\": \"basic-auth-user\",
    \"input\": \"Testing basic auth format\"
  }" \
  -w "\nStatus: %{http_code}\nTime: %{time_total}s\n" | jq '.'

echo ""
echo "============================================================"

# =================================
# 5. Test with X-API-Key Header
# =================================
echo ""
echo "5️⃣ Testing X-API-Key Header Format..."

curl -X POST "$LANGFUSE_HOST/api/public/traces" \
  -H "X-API-Key: $LANGFUSE_SECRET_KEY" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d "{
    \"id\": \"$(uuidgen | tr '[:upper:]' '[:lower:]')\",
    \"name\": \"x-api-key-test\",
    \"userId\": \"x-api-key-user\",
    \"input\": \"Testing X-API-Key format\"
  }" \
  -w "\nStatus: %{http_code}\nTime: %{time_total}s\n" | jq '.'

echo ""
echo "============================================================"

# =================================
# 6. Test Direct API Call (No Auth)
# =================================
echo ""
echo "6️⃣ Testing No Auth (Check if local allows it)..."

curl -X GET "$LANGFUSE_HOST/api/public/traces" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -w "\nStatus: %{http_code}\nTime: %{time_total}s\n"

echo ""
echo "============================================================"

# =================================
# 7. Test Ingestion API (Alternative Endpoint)
# =================================
echo ""
echo "7️⃣ Testing Ingestion API..."

curl -X POST "$LANGFUSE_HOST/api/public/ingestion" \
  -u "$LANGFUSE_PUBLIC_KEY:$LANGFUSE_SECRET_KEY" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d "{
    \"batch\": [
      {
        \"id\": \"$(uuidgen | tr '[:upper:]' '[:lower:]')\",
        \"type\": \"trace-create\",
        \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\",
        \"body\": {
          \"id\": \"$(uuidgen | tr '[:upper:]' '[:lower:]')\",
          \"name\": \"ingestion-test\",
          \"userId\": \"ingestion-user\",
          \"input\": \"Testing ingestion API\"
        }
      }
    ]
  }" \
  -w "\nStatus: %{http_code}\nTime: %{time_total}s\n" | jq '.'

echo ""
echo "============================================================"

# =================================
# 8. Check Local LangFuse Configuration
# =================================
echo ""
echo "8️⃣ Checking Local LangFuse Config..."

echo "Testing if local instance is properly configured:"
curl -X GET "$LANGFUSE_HOST" \
  -H "Accept: text/html" \
  -w "\nStatus: %{http_code}\n" \
  -s | grep -i "langfuse\|setup\|configuration" | head -3

echo ""
echo "============================================================"

# =================================
# 9. Test Your Exact Spring Boot Configuration
# =================================
echo ""
echo "9️⃣ Testing Your Spring Boot App Configuration..."

# This matches exactly what your Spring Boot app is trying to do
curl -X POST "$LANGFUSE_HOST/api/public/traces" \
  -H "Authorization: Bearer $LANGFUSE_SECRET_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"id\": \"spring-boot-test-$(date +%s)\",
    \"name\": \"chat-completion\",
    \"userId\": \"spring-test-user\",
    \"sessionId\": \"spring-test-session\",
    \"input\": \"Test from Spring Boot config\",
    \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\",
    \"metadata\": {
      \"source\": \"spring-ai-demo\",
      \"version\": \"1.0.0\"
    }
  }" \
  -v 2>&1

echo ""
echo "============================================================"

# =================================
# 10. Debug: Check What Auth Method Works
# =================================
echo ""
echo "🔟 Debug: Finding Working Auth Method..."

echo ""
echo "Method 1: Bearer token"
curl -X GET "$LANGFUSE_HOST/api/public/health" \
  -H "Authorization: Bearer $LANGFUSE_SECRET_KEY" \
  -w "Status: %{http_code}\n" -s | head -1

echo ""
echo "Method 2: Basic Auth"
curl -X GET "$LANGFUSE_HOST/api/public/health" \
  -u "$LANGFUSE_PUBLIC_KEY:$LANGFUSE_SECRET_KEY" \
  -w "Status: %{http_code}\n" -s | head -1

echo ""
echo "Method 3: Basic Auth (reversed)"
curl -X GET "$LANGFUSE_HOST/api/public/health" \
  -u "$LANGFUSE_SECRET_KEY:$LANGFUSE_PUBLIC_KEY" \
  -w "Status: %{http_code}\n" -s | head -1

echo ""
echo "Method 4: No Auth"
curl -X GET "$LANGFUSE_HOST/api/public/health" \
  -w "Status: %{http_code}\n" -s | head -1

echo ""
echo "============================================================"

echo ""
echo "🔧 DIAGNOSIS BASED ON YOUR SETUP:"
echo "================================="
echo "✅ LangFuse is running locally on localhost:3000"
echo "✅ Health endpoint works (status: OK, version: 3.88.1)"
echo "❌ Authentication is failing - local instance needs different auth"
echo ""
echo "🚀 SOLUTIONS TO TRY:"
echo "==================="
echo "1. Check if your local LangFuse requires different authentication"
echo "2. Try Basic Auth instead of Bearer token in Spring Boot"
echo "3. Check local LangFuse documentation for API auth"
echo "4. Verify the API keys are correct in your local instance"
echo ""
echo "💡 FOR YOUR SPRING BOOT APP:"
echo "============================="
echo "Update your LangFuseService.java to use Basic Auth:"
echo ""
echo "private WebClient getWebClient() {"
echo "    String credentials = publicKey + \":\" + secretKey;"
echo "    String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());"
echo "    return webClientBuilder"
echo "            .baseUrl(langfuseHost)"
echo "            .defaultHeader(\"Authorization\", \"Basic \" + encodedCredentials)"
echo "            .defaultHeader(\"Content-Type\", \"application/json\")"
echo "            .build();"
echo "}"