param([string]$KeycloakUrl = 'http://localhost:8083')
$ErrorActionPreference = 'Stop'
$composeRoot = Split-Path -Parent $PSScriptRoot
$settings = @{}
foreach ($line in Get-Content -LiteralPath (Join-Path $composeRoot '.env')) {
    if ($line -match '^([A-Z_]+)=(.*)$') { $settings[$Matches[1]] = $Matches[2].Trim().Trim('"').Trim("'") }
}
foreach ($required in @('KEYCLOAK_ADMIN_USERNAME', 'KEYCLOAK_ADMIN_PASSWORD', 'MAIL_KEYCLOAK_CLIENT_SECRET')) {
    if (-not $settings[$required]) { throw "Set $required in docker-compose/.env before running this script." }
}
$token = Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" -Body @{
    grant_type = 'password'; client_id = 'admin-cli'
    username = $settings['KEYCLOAK_ADMIN_USERNAME']; password = $settings['KEYCLOAK_ADMIN_PASSWORD']
}
$headers = @{ Authorization = 'Bearer ' + $token.access_token }
$realmUrl = "$KeycloakUrl/admin/realms/bidding"
$template = Get-Content -Raw -LiteralPath (Join-Path $composeRoot 'keycloak/bidding-realm.json') | ConvertFrom-Json
$client = $template.clients | Where-Object clientId -eq 'mail-service'
$client.secret = $settings['MAIL_KEYCLOAK_CLIENT_SECRET']
$existing = @(Invoke-RestMethod -Uri "$realmUrl/clients?clientId=mail-service" -Headers $headers)
if ($existing.Count -eq 0) {
    $null = Invoke-RestMethod -Method Post -Uri "$realmUrl/clients" -Headers $headers -ContentType 'application/json' -Body ($client | ConvertTo-Json -Depth 15)
    $existing = @(Invoke-RestMethod -Uri "$realmUrl/clients?clientId=mail-service" -Headers $headers)
} else {
    $null = Invoke-RestMethod -Method Put -Uri "$realmUrl/clients/$($existing[0].id)" -Headers $headers -ContentType 'application/json' -Body ($client | ConvertTo-Json -Depth 15)
}
$scope = @(Invoke-RestMethod -Uri "$realmUrl/client-scopes" -Headers $headers) | Where-Object name -eq 'bidding-api'
if (-not $scope) { throw 'Configure the bidding-api audience scope before configuring the mail client.' }
$null = Invoke-RestMethod -Method Put -Uri "$realmUrl/clients/$($existing[0].id)/default-client-scopes/$($scope.id)" -Headers $headers
Write-Output 'Configured mail-service with client credentials and the bidding-api audience. No admin roles are granted.'
