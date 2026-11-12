param(
    [string]$PostgresBin = 'C:\Program Files\PostgreSQL\18\bin'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
foreach ($binary in @('initdb.exe', 'pg_ctl.exe', 'createdb.exe')) {
    if (-not (Test-Path -LiteralPath (Join-Path $PostgresBin $binary))) {
        throw "Missing $binary. Set -PostgresBin to the PostgreSQL bin directory."
    }
}

$portProbe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
$portProbe.Start()
$demoPort = $portProbe.LocalEndpoint.Port
$portProbe.Stop()
$demoRoot = Join-Path $projectRoot ('target/discord-study-pg-' + [Guid]::NewGuid().ToString('N'))
$demoData = Join-Path $demoRoot 'data'
New-Item -ItemType Directory -Path $demoRoot -Force | Out-Null
$savedTestEnvironment = @{}
foreach ($name in @('TEST_DB_URL', 'TEST_DB_USERNAME', 'TEST_DB_PASSWORD')) {
    $savedTestEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

Push-Location $projectRoot
try {
    & (Join-Path $PostgresBin 'initdb.exe') -D $demoData -U postgres -A trust --encoding=UTF8 --locale=C *> (Join-Path $demoRoot 'initdb.log')
    if ($LASTEXITCODE -ne 0) { throw 'initdb failed; see the initdb.log in the demo directory.' }
    $startArguments = @('-D', ('"' + $demoData + '"'), '-l', ('"' + (Join-Path $demoRoot 'postgres.log') + '"'),
        '-o', ('"-p ' + $demoPort + ' -h 127.0.0.1"'), '-w', 'start')
    $pgStart = Start-Process -FilePath (Join-Path $PostgresBin 'pg_ctl.exe') -ArgumentList $startArguments `
        -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $demoRoot 'pg-start.log') `
        -RedirectStandardError (Join-Path $demoRoot 'pg-start-error.log')
    # Start-Process -Wait also waits for the server's descendants on Windows.
    if (-not $pgStart.WaitForExit(30000)) { throw 'Timed out waiting for pg_ctl.' }
    & (Join-Path $PostgresBin 'pg_ctl.exe') -D $demoData status *> (Join-Path $demoRoot 'pg-status.log')
    if ($LASTEXITCODE -ne 0) { throw 'Temporary PostgreSQL did not start; see pg-start-error.log.' }
    & (Join-Path $PostgresBin 'createdb.exe') -h 127.0.0.1 -p $demoPort -U postgres discord_study_test
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the temporary test database.' }

    $env:TEST_DB_URL = "jdbc:postgresql://127.0.0.1:$demoPort/discord_study_test"
    $env:TEST_DB_USERNAME = 'postgres'
    $env:TEST_DB_PASSWORD = 'unused-local-test-password'
    Write-Host 'Running JDA listener -> transactional services -> isolated PostgreSQL (no Discord connection).'
    # Windows PowerShell otherwise turns JVM stderr warnings into terminating errors when redirected.
    $ErrorActionPreference = 'Continue'
    & mvn -B '-Dtest=StudySessionServiceIntegrationTest,UserIdentityServiceIntegrationTest,SqlSchemaTest,EntityMappingTest' package 2>&1 |
        Tee-Object -FilePath (Join-Path $demoRoot 'maven.log')
    $mavenExitCode = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($mavenExitCode -ne 0) { throw 'Discord study checks failed. See target/surefire-reports.' }
    Write-Host 'Listener scenario result: 1 user, 1 identity, 2 closed sessions and 4 closed segments, with selective visual coverage.'
    Write-Host 'Verified: join, camera/share on/off, room move, leave, bot exclusion, room filtering, rollback and SQL mappings.'
}
finally {
    if (Test-Path -LiteralPath (Join-Path $demoData 'postmaster.pid')) {
        & (Join-Path $PostgresBin 'pg_ctl.exe') -D $demoData -m fast -w stop
    }
    foreach ($name in $savedTestEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($name, $savedTestEnvironment[$name], 'Process')
    }
    Pop-Location
    Write-Host "Temporary PostgreSQL files: $demoRoot"
}
