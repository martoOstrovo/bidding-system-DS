# Upserts only the gateway client; startup realm imports skip existing realms.
$ErrorActionPreference = 'Stop'
$composeRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $composeRoot '.env'
$envText = [System.IO.File]::ReadAllText($envPath)
$clientSecret = [regex]::Match($envText, '(?m)^GATEWAY_OAUTH2_CLIENT_SECRET=(.*)$').Groups[1].Value.Trim()
if (-not $clientSecret) {
    $secretBytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($secretBytes)
    $rng.Dispose()
    $clientSecret = [Convert]::ToBase64String($secretBytes)
    if ($envText -match '(?m)^GATEWAY_OAUTH2_CLIENT_SECRET=') {
        $envText = [regex]::Replace($envText, '(?m)^GATEWAY_OAUTH2_CLIENT_SECRET=.*$', "GATEWAY_OAUTH2_CLIENT_SECRET=$clientSecret")
    } else {
        $envText += "`r`nGATEWAY_OAUTH2_CLIENT_SECRET=$clientSecret`r`n"
    }
    [System.IO.File]::WriteAllText($envPath, $envText)
}
$adminUser = [regex]::Match($envText, '(?m)^KEYCLOAK_ADMIN_USERNAME=(.*)$').Groups[1].Value.Trim()
$adminPassword = [regex]::Match($envText, '(?m)^KEYCLOAK_ADMIN_PASSWORD=(.*)$').Groups[1].Value.Trim()
$adminToken = Invoke-RestMethod -Method Post -Uri 'http://localhost:8083/realms/master/protocol/openid-connect/token' -Body @{
    grant_type = 'password'; client_id = 'admin-cli'; username = $adminUser; password = $adminPassword
}
$headers = @{ Authorization = 'Bearer ' + $adminToken.access_token }
$realm = Get-Content -Raw (Join-Path $composeRoot 'keycloak/bidding-realm.json') | ConvertFrom-Json
$existingScopes = Invoke-RestMethod -Uri 'http://localhost:8083/admin/realms/bidding/client-scopes' -Headers $headers
foreach ($scopeTemplate in $realm.clientScopes) {
    if (-not ($existingScopes | Where-Object name -eq $scopeTemplate.name)) {
        $null = Invoke-RestMethod -Method Post -Uri 'http://localhost:8083/admin/realms/bidding/client-scopes' -Headers $headers `
            -ContentType 'application/json' -Body ($scopeTemplate | ConvertTo-Json -Depth 15)
    }
}
$client = $realm.clients | Where-Object clientId -eq 'bidding-gateway'
$client.secret = $clientSecret
$existingResponse = Invoke-RestMethod -Uri 'http://localhost:8083/admin/realms/bidding/clients?clientId=bidding-gateway' -Headers $headers
$existing = @($existingResponse | Where-Object clientId -eq 'bidding-gateway')
if ($existing.Count -gt 0) {
    $id = $existing[0].id
    $null = Invoke-RestMethod -Method Put -Uri "http://localhost:8083/admin/realms/bidding/clients/$id" -Headers $headers `
        -ContentType 'application/json' -Body ($client | ConvertTo-Json -Depth 15)
} else {
    $null = Invoke-RestMethod -Method Post -Uri 'http://localhost:8083/admin/realms/bidding/clients' -Headers $headers `
        -ContentType 'application/json' -Body ($client | ConvertTo-Json -Depth 15)
    $created = @(Invoke-RestMethod -Uri 'http://localhost:8083/admin/realms/bidding/clients?clientId=bidding-gateway' -Headers $headers)
    $id = $created[0].id
}
$scopes = Invoke-RestMethod -Uri 'http://localhost:8083/admin/realms/bidding/client-scopes' -Headers $headers
foreach ($scope in $scopes | Where-Object { $_.name -in $client.defaultClientScopes }) {
    $null = Invoke-RestMethod -Method Put -Uri "http://localhost:8083/admin/realms/bidding/clients/$id/default-client-scopes/$($scope.id)" -Headers $headers
}
Write-Output 'Configured bidding-gateway: Authorization Code, confidential client, required PKCE S256.'
Write-Output 'The client secret is stored in the ignored docker-compose/.env file.'
