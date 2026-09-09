# Apply the account client to an existing realm without deleting users or volumes.
param([string]$KeycloakUrl = 'http://localhost:8083')
$ErrorActionPreference = 'Stop'
$composeRoot = Split-Path -Parent $PSScriptRoot
$settings = @{}
foreach ($line in Get-Content -LiteralPath (Join-Path $composeRoot '.env')) {
    if ($line -match '^([A-Z_]+)=(.*)$') { $settings[$Matches[1]] = $Matches[2].Trim().Trim('"').Trim("'") }
}
foreach ($required in @('KEYCLOAK_ADMIN_USERNAME', 'KEYCLOAK_ADMIN_PASSWORD', 'ACCOUNT_KEYCLOAK_CLIENT_SECRET')) {
    if (-not $settings[$required]) { throw "Set $required in docker-compose/.env before running this script." }
}
$token = Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" -Body @{
    grant_type = 'password'; client_id = 'admin-cli'
    username = $settings['KEYCLOAK_ADMIN_USERNAME']; password = $settings['KEYCLOAK_ADMIN_PASSWORD']
}
$headers = @{ Authorization = 'Bearer ' + $token.access_token }
$realmUrl = "$KeycloakUrl/admin/realms/bidding"
$template = Get-Content -Raw -LiteralPath (Join-Path $composeRoot 'keycloak/bidding-realm.json') | ConvertFrom-Json
$client = $template.clients | Where-Object clientId -eq 'account-service'
$client.secret = $settings['ACCOUNT_KEYCLOAK_CLIENT_SECRET']
$existing = @(Invoke-RestMethod -Uri "$realmUrl/clients?clientId=account-service" -Headers $headers)
if ($existing.Count -eq 0) {
    $null = Invoke-RestMethod -Method Post -Uri "$realmUrl/clients" -Headers $headers -ContentType 'application/json' -Body ($client | ConvertTo-Json -Depth 15)
    $existing = @(Invoke-RestMethod -Uri "$realmUrl/clients?clientId=account-service" -Headers $headers)
} else {
    $null = Invoke-RestMethod -Method Put -Uri "$realmUrl/clients/$($existing[0].id)" -Headers $headers -ContentType 'application/json' -Body ($client | ConvertTo-Json -Depth 15)
}
$serviceAccount = Invoke-RestMethod -Uri "$realmUrl/clients/$($existing[0].id)/service-account-user" -Headers $headers
$management = @(Invoke-RestMethod -Uri "$realmUrl/clients?clientId=realm-management" -Headers $headers)[0]
$roles = @(
    foreach ($role in @('manage-users', 'view-users')) {
        Invoke-RestMethod -Uri "$realmUrl/clients/$($management.id)/roles/$role" -Headers $headers
    }
)
$null = Invoke-RestMethod -Method Post -Uri "$realmUrl/users/$($serviceAccount.id)/role-mappings/clients/$($management.id)" -Headers $headers -ContentType 'application/json' -Body (ConvertTo-Json -InputObject $roles -Depth 10)
Write-Output 'Configured account-service with client credentials and user management permissions.'
