# End-to-end Authorization Code + PKCE test. Uses and removes a temporary user.
param([string]$FrontendUrl = 'http://localhost:5173', [switch]$CheckAccountProfile = $true)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$keycloakUrl = 'http://localhost:8083'
$gatewayUrl = 'http://localhost:8090'
$envText = [System.IO.File]::ReadAllText((Join-Path (Split-Path -Parent $PSScriptRoot) '.env'))
$adminUser = [regex]::Match($envText, '(?m)^KEYCLOAK_ADMIN_USERNAME=(.*)$').Groups[1].Value.Trim()
$adminPassword = [regex]::Match($envText, '(?m)^KEYCLOAK_ADMIN_PASSWORD=(.*)$').Groups[1].Value.Trim()
$testUsername = 'bff-smoke-' + [guid]::NewGuid().ToString('N')
$testPassword = [guid]::NewGuid().ToString('N') + 'Aa1!'
$testUserId = $null
$profileRequested = $false
$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.AllowAutoRedirect = $false
$browser = [System.Net.Http.HttpClient]::new($handler)
$browser.Timeout = [TimeSpan]::FromSeconds(15)

function Check-Status($Response, [int]$Expected, [string]$Step) {
    if ([int]$Response.StatusCode -ne $Expected) { throw "$Step expected $Expected, received $([int]$Response.StatusCode)." }
    Write-Output "$Step passed ($Expected)."
}

function Use-BrowserLocalhostCookieBehavior {
    # Browsers treat http://localhost as a trustworthy context for Secure cookies.
    # .NET's cookie jar does not. Emulate that exception only in this local test.
    foreach ($cookie in $handler.CookieContainer.GetCookies([uri]'https://localhost:8083/realms/bidding/protocol/openid-connect/auth')) {
        if ($cookie.Domain -eq 'localhost' -and $cookie.Path.StartsWith('/realms/bidding')) {
            $cookie.Secure = $false
        }
    }
}

try {
    $adminToken = Invoke-RestMethod -Method Post -Uri "$keycloakUrl/realms/master/protocol/openid-connect/token" -Body @{
        grant_type = 'password'; client_id = 'admin-cli'; username = $adminUser; password = $adminPassword
    }
    $adminHeaders = @{ Authorization = 'Bearer ' + $adminToken.access_token }
    $user = @{
        username = $testUsername; enabled = $true; firstName = 'Login'; lastName = 'Smoke Test'
        email = "$testUsername@example.com"; emailVerified = $true; requiredActions = @()
        credentials = @(@{ type = 'password'; value = $testPassword; temporary = $false })
    }
    $created = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$keycloakUrl/admin/realms/bidding/users" `
        -Headers $adminHeaders -ContentType 'application/json' -Body ($user | ConvertTo-Json -Depth 5)
    $testUserId = ([string]$created.Headers.Location).TrimEnd('/').Split('/')[-1]
    if (-not $testUserId) { throw 'Missing temporary user ID.' }

    $login = $browser.GetAsync("$gatewayUrl/oauth2/authorization/keycloak").GetAwaiter().GetResult()
    Check-Status $login 302 'Login redirect'
    $authorizationUrl = $login.Headers.Location.AbsoluteUri
    if ($authorizationUrl -notmatch 'code_challenge_method=S256' -or $authorizationUrl -notmatch 'code_challenge=') {
        throw 'The authorization request did not include PKCE S256.'
    }
    if ($authorizationUrl -match 'client_secret=') { throw 'The browser received a client secret.' }
    $login.Dispose()
    $formResponse = $browser.GetAsync($authorizationUrl).GetAwaiter().GetResult()
    Check-Status $formResponse 200 'Keycloak login page'
    $html = $formResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $formTag = [regex]::Match($html, '<form\b[^>]*id="kc-form-login"[^>]*>').Value
    $formAction = [System.Net.WebUtility]::HtmlDecode([regex]::Match($formTag, 'action="([^"]+)"').Groups[1].Value)
    if (-not $formAction -or ([uri]$formAction).Authority -ne 'localhost:8083') { throw 'Unexpected login form action.' }
    $formResponse.Dispose()
    Use-BrowserLocalhostCookieBehavior
    $fields = [System.Collections.Generic.Dictionary[string,string]]::new()
    $fields.Add('username', $testUsername)
    $fields.Add('password', $testPassword)
    $fields.Add('credentialId', '')
    $content = [System.Net.Http.FormUrlEncodedContent]::new($fields)
    $authenticated = $browser.PostAsync($formAction, $content).GetAwaiter().GetResult()
    Check-Status $authenticated 302 'Keycloak authorization code'
    $callback = $authenticated.Headers.Location.AbsoluteUri
    if (-not $callback.StartsWith("$gatewayUrl/login/oauth2/code/keycloak?")) { throw 'Unexpected OAuth callback.' }
    $authenticated.Dispose()
    Use-BrowserLocalhostCookieBehavior
    $callbackResponse = $browser.GetAsync($callback).GetAwaiter().GetResult()
    Check-Status $callbackResponse 302 'Gateway callback'
    if (([string]$callbackResponse.Headers.Location).TrimEnd('/') -ne $FrontendUrl.TrimEnd('/')) {
        $redirectPath = ([string]$callbackResponse.Headers.Location).Split('?')[0]
        throw "The gateway returned to '$redirectPath' instead of the frontend. Rebuild/restart the gateway and check FRONTEND_URL."
    }
    $callbackResponse.Dispose()
    $cookie = $handler.CookieContainer.GetCookies([uri]$gatewayUrl)['BIDDING_SESSION']
    if (-not $cookie -or -not $cookie.HttpOnly) { throw 'Missing HttpOnly gateway session cookie.' }
    $meResponse = $browser.GetAsync("$gatewayUrl/auth/me").GetAwaiter().GetResult()
    Check-Status $meResponse 200 'Session identity'
    $identityText = $meResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $identity = $identityText | ConvertFrom-Json
    if ($identity.id -ne $testUserId -or $identity.username -ne $testUsername) { throw 'Session identity did not match the user.' }
    if ($identityText -match 'access_token|refresh_token|id_token|eyJ') { throw 'Identity response exposed token data.' }
    if (-not @($identity.authorities | Where-Object { $_ -like 'ROLE_*' }).Count) { throw 'OIDC realm roles were not mapped.' }
    $meResponse.Dispose()

    if ($CheckAccountProfile) {
        $profileRequested = $true
        $profileResponse = $browser.GetAsync("$gatewayUrl/account-service/api/me").GetAwaiter().GetResult()
        Check-Status $profileResponse 200 'Persisted account profile'
        $profile = $profileResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
        if ($profile.userId -ne $testUserId -or $profile.username -ne $testUsername) { throw 'The persisted account profile did not match the test identity.' }
        $profileResponse.Dispose()
        $profileCsrfResponse = $browser.GetAsync("$gatewayUrl/auth/csrf").GetAwaiter().GetResult()
        $profileCsrf = $profileCsrfResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
        $profileCsrfResponse.Dispose()
        $deleteProfile = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Delete, "$gatewayUrl/account-service/api/me")
        $deleteProfile.Headers.Add($profileCsrf.headerName, $profileCsrf.token)
        $deletedProfile = $browser.SendAsync($deleteProfile).GetAwaiter().GetResult()
        Check-Status $deletedProfile 204 'Test account cleanup'
        $deletedProfile.Dispose()
        $deleteProfile.Dispose()
        $profileRequested = $false
        $testUserId = $null
    }

    $api = $browser.GetAsync("$gatewayUrl/item-service/api/get/not-a-uuid").GetAwaiter().GetResult()
    Check-Status $api 400 'Session-authenticated API routing'
    $api.Dispose()
    $withoutCsrf = $browser.PostAsync("$gatewayUrl/logout", [System.Net.Http.StringContent]::new('')).GetAwaiter().GetResult()
    Check-Status $withoutCsrf 403 'Logout without CSRF rejected'
    $withoutCsrf.Dispose()
    $csrfResponse = $browser.GetAsync("$gatewayUrl/auth/csrf").GetAwaiter().GetResult()
    Check-Status $csrfResponse 200 'CSRF token'
    $csrf = $csrfResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
    $csrfResponse.Dispose()
    $write = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post, "$gatewayUrl/auth/me")
    $write.Headers.Add($csrf.headerName, $csrf.token)
    $writeResult = $browser.SendAsync($write).GetAwaiter().GetResult()
    # This endpoint only accepts GET; 405 proves the session and CSRF checks passed.
    Check-Status $writeResult 405 'Session write with CSRF accepted by security'
    $writeResult.Dispose()
    $write.Dispose()
    $logout = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post, "$gatewayUrl/logout")
    $logout.Headers.Add($csrf.headerName, $csrf.token)
    $logoutResponse = $browser.SendAsync($logout).GetAwaiter().GetResult()
    Check-Status $logoutResponse 302 'Logout redirect'
    $next = $logoutResponse.Headers.Location.AbsoluteUri
    $logoutResponse.Dispose()
    $logout.Dispose()
    for ($i = 0; $i -lt 5; $i++) {
        if ($next -eq $FrontendUrl -or $next -eq "$FrontendUrl/") { break }
        if (([uri]$next).Authority -notin @('localhost:8083', 'localhost:8090')) { throw 'Unexpected logout redirect host.' }
        $response = $browser.GetAsync($next).GetAwaiter().GetResult()
        if ([int]$response.StatusCode -eq 200) {
            $response.Dispose()
            break
        }
        Check-Status $response 302 'Keycloak logout'
        $next = [uri]::new([uri]$next, $response.Headers.Location).AbsoluteUri
        $response.Dispose()
    }
    $afterLogout = $browser.GetAsync("$gatewayUrl/auth/me").GetAwaiter().GetResult()
    Check-Status $afterLogout 401 'Session invalidated'
    $afterLogout.Dispose()
    Write-Output 'BFF login, PKCE, session, role mapping, CSRF, and logout checks passed.'
} finally {
    if ($profileRequested -and $testUserId) {
        try {
            $cleanupCsrfResponse = $browser.GetAsync("$gatewayUrl/auth/csrf").GetAwaiter().GetResult()
            $cleanupCsrf = $cleanupCsrfResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
            $cleanupCsrfResponse.Dispose()
            $cleanupRequest = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Delete, "$gatewayUrl/account-service/api/me")
            $cleanupRequest.Headers.Add($cleanupCsrf.headerName, $cleanupCsrf.token)
            $cleanupResponse = $browser.SendAsync($cleanupRequest).GetAwaiter().GetResult()
            if ([int]$cleanupResponse.StatusCode -eq 204) { $testUserId = $null; Write-Output 'Removed temporary account profile and Keycloak user.' }
            else { Write-Warning 'Test profile cleanup did not succeed; the temporary local profile may need cleanup.' }
            $cleanupResponse.Dispose()
            $cleanupRequest.Dispose()
        } catch { Write-Warning 'Could not clean up the temporary local account profile.' }
    }
    if ($testUserId) {
        $null = Invoke-RestMethod -Method Delete -Uri "$keycloakUrl/admin/realms/bidding/users/$testUserId" -Headers $adminHeaders
        Write-Output 'Removed temporary login test user.'
    }
    $browser.Dispose()
    $handler.Dispose()
}
