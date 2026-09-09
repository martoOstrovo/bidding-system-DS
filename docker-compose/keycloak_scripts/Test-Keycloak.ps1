# Creates a temporary service-account client, tests real JWTs, and removes it.
# Does not create application users or send emails.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$keycloakUrl = 'http://localhost:8083'
$gatewayUrl = 'http://localhost:8090'
$envText = [System.IO.File]::ReadAllText((Join-Path $PSScriptRoot '.env'))
$adminUser = [regex]::Match($envText, '(?m)^KEYCLOAK_ADMIN_USERNAME=(.*)$').Groups[1].Value.Trim()
$adminPassword = [regex]::Match($envText, '(?m)^KEYCLOAK_ADMIN_PASSWORD=(.*)$').Groups[1].Value.Trim()
if (-not $adminUser -or -not $adminPassword) { throw 'Keycloak admin credentials are missing from .env.' }
$temporaryClientName = 'gateway-smoke-' + [guid]::NewGuid().ToString('N')
$temporarySecret = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$temporaryClientId = $null
$http = [System.Net.Http.HttpClient]::new()

function Assert-Status([string]$Path, [int]$Expected, [string]$AccessToken = '') {
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get, "$gatewayUrl$Path")
    if ($AccessToken) {
        $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $AccessToken)
    }
    try {
        $response = $http.SendAsync($request).GetAwaiter().GetResult()
        try {
            if ([int]$response.StatusCode -ne $Expected) {
                throw "Expected $Expected for $Path, received $([int]$response.StatusCode)."
            }
            Write-Output "$Expected $Path"
        } finally { $response.Dispose() }
    } finally { $request.Dispose() }
}

try {
    $adminToken = Invoke-RestMethod -Method Post -Uri "$keycloakUrl/realms/master/protocol/openid-connect/token" -Body @{
        grant_type = 'password'; client_id = 'admin-cli'; username = $adminUser; password = $adminPassword
    }
    $adminHeaders = @{ Authorization = 'Bearer ' + $adminToken.access_token }
    $clientRepresentation = @{
        clientId = $temporaryClientName; secret = $temporarySecret; protocol = 'openid-connect'
        enabled = $true; publicClient = $false; serviceAccountsEnabled = $true
        standardFlowEnabled = $false; directAccessGrantsEnabled = $false
        defaultClientScopes = @('bidding-api')
    }
    $created = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$keycloakUrl/admin/realms/bidding/clients" `
        -Headers $adminHeaders -ContentType 'application/json' -Body ($clientRepresentation | ConvertTo-Json -Depth 5)
    $temporaryClientId = ([string]$created.Headers.Location).TrimEnd('/').Split('/')[-1]
    if (-not $temporaryClientId) { throw 'Keycloak did not return the created client ID.' }
    $token = Invoke-RestMethod -Method Post -Uri "$keycloakUrl/realms/bidding/protocol/openid-connect/token" -Body @{
        grant_type = 'client_credentials'; client_id = $temporaryClientName; client_secret = $temporarySecret
    }
    Assert-Status '/actuator/health/readiness' 200
    Assert-Status '/item-service/swagger-ui/index.html' 200
    Assert-Status '/bidding-service/v3/api-docs' 200
    Assert-Status '/item-service/api/get/not-a-uuid' 401
    # 400 is the item's UUID validation response, proving authentication and routing succeeded.
    Assert-Status '/item-service/api/get/not-a-uuid' 400 $token.access_token
    Assert-Status '/item-service/actuator/env' 403 $token.access_token

    $assignedScopes = Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/bidding/clients/$temporaryClientId/default-client-scopes" -Headers $adminHeaders
    $apiScopeId = ($assignedScopes | Where-Object name -eq 'bidding-api').id
    if (-not $apiScopeId) { throw 'The temporary client is missing the expected audience scope.' }
    $null = Invoke-RestMethod -Method Delete -Uri "$keycloakUrl/admin/realms/bidding/clients/$temporaryClientId/default-client-scopes/$apiScopeId" -Headers $adminHeaders
    $wrongAudienceToken = Invoke-RestMethod -Method Post -Uri "$keycloakUrl/realms/bidding/protocol/openid-connect/token" -Body @{
        grant_type = 'client_credentials'; client_id = $temporaryClientName; client_secret = $temporarySecret
    }
    Assert-Status '/item-service/api/get/not-a-uuid' 401 $wrongAudienceToken.access_token
    Write-Output 'Keycloak and gateway smoke checks passed.'
} finally {
    if ($temporaryClientId) {
        $null = Invoke-RestMethod -Method Delete -Uri "$keycloakUrl/admin/realms/bidding/clients/$temporaryClientId" -Headers $adminHeaders
        Write-Output 'Removed temporary test client.'
    }
    $http.Dispose()
}
