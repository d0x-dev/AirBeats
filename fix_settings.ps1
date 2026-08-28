$content = Get-Content -Path app\src\main\java\com\darkxvenom\airbeats\ui\screens\settings\SettingsScreen.kt -Raw
$pattern = '(?s)var downloadStatus by remember \{ mutableStateOf\(DownloadStatus.NOT_STARTED\) \}.*?downloadStatus = DownloadStatus.COMPLETED[\r\n\s]+onDismiss\(\)[\r\n\s]+\},'
$replacement = "
    var downloadStatus by remember { mutableStateOf(DownloadStatus.NOT_STARTED) }

    Dialog(onDismissRequest = {
        if (downloadStatus != DownloadStatus.REDIRECTING) {
            onDismiss()
        }
    }) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.update_version, latestVersion),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (downloadStatus) {
                    DownloadStatus.NOT_STARTED -> {
                        Text(
                            stringResource(R.string.download_question),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WaterDropButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.cancel),
                                colors = listOf(
                                    Color(0xFF2A2A2A),
                                    Color(0xFF3A3A3A),
                                    Color(0xFF2A2A2A)
                                )
                            )

                            WaterDropButton(
                                onClick = {
                                    downloadStatus = DownloadStatus.REDIRECTING
                                    val downloadUrl = if (com.darkxvenom.airbeats.BuildConfig.IS_NIGHTLY) {
                                        \"https://github.com/d0x-dev/AirBeats/releases/download/v$latestVersion-nightly/Airbeats-v$latestVersion-Nightly.apk\"
                                    } else {
                                        \"https://github.com/d0x-dev/AirBeats/releases/download/v$latestVersion/AirBeats_v${latestVersion}_signed.apk\"
                                    }
                                    uriHandler.openUri(downloadUrl)
                                    downloadStatus = DownloadStatus.COMPLETED
                                    onDismiss()
                                },
"
$content = $content -replace $pattern, $replacement
Set-Content -Path app\src\main\java\com\darkxvenom\airbeats\ui\screens\settings\SettingsScreen.kt -Value $content
