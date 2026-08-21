file_path = 'app/src/main/java/com/darkxvenom/airbeats/viewmodels/NewReleaseViewModel.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Remove the stray '    }' before 'sealed interface NewReleaseUiState'
for i, line in enumerate(lines):
    if line.strip() == '}' and 'sealed interface NewReleaseUiState' in ''.join(lines[i:]):
        lines.pop(i)
        break

with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
