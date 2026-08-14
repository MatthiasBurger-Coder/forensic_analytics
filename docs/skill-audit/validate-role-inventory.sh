#!/usr/bin/env bash

set -euo pipefail

usage() {
    cat <<'EOF'
Usage: validate-role-inventory.sh [--roles-root PATH]

Validate direct project-role entry points below .agents/roles without changing
any source files. --roles-root can point to an isolated roles directory for
controlled validation checks.
EOF
}

script_directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
roles_root="$script_directory/../../.agents/roles"
display_root=".agents/roles"

while (($# > 0)); do
    case "$1" in
        --roles-root)
            if (($# < 2)); then
                printf 'Actionable failures (1):\n- [INVALID_ARGUMENT] --roles-root requires a directory path.\n' >&2
                exit 1
            fi
            roles_root="$2"
            display_root="$2"
            shift 2
            ;;
        --roles-root=*)
            roles_root="${1#*=}"
            display_root="$roles_root"
            shift
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            printf 'Actionable failures (1):\n- [INVALID_ARGUMENT] Unexpected argument: %s.\n' "$1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

if [[ "$display_root" != "/" ]]; then
    display_root="${display_root%/}"
fi

if ! roles_root_absolute="$(cd -- "$roles_root" 2>/dev/null && pwd -P)"; then
    printf 'Role inventory root: %s\n' "$display_root"
    printf 'Actionable failures (1):\n'
    printf -- '- [MISSING_ROLES_ROOT] Roles root does not exist or is not a directory: %s\n' "$display_root"
    exit 1
fi

display_path() {
    local absolute_path="$1"
    local relative_path="${absolute_path#"$roles_root_absolute"/}"
    printf '%s/%s' "$display_root" "$relative_path"
}

direct_entries=()
flat_entries=()
directory_entries=()
unexpected_entries=()
missing_entries=()

mapfile -d '' -t direct_entries < <(
    find "$roles_root_absolute" -mindepth 1 -maxdepth 1 -print0 | LC_ALL=C sort -z
)

for entry in "${direct_entries[@]}"; do
    if [[ -f "$entry" && ! -L "$entry" && "$entry" == *.md ]]; then
        flat_entries+=("$entry")
    elif [[ -d "$entry" && ! -L "$entry" ]]; then
        if [[ -f "$entry/SKILL.md" && ! -L "$entry/SKILL.md" ]]; then
            directory_entries+=("$entry")
        else
            unexpected_entries+=("$entry")
            missing_entries+=("$entry/SKILL.md")
        fi
    else
        unexpected_entries+=("$entry")
    fi
done

logical_names=()
logical_records=()

for entry in "${flat_entries[@]}"; do
    filename="${entry##*/}"
    logical_name="${filename%.md}"
    logical_names+=("$logical_name")
    logical_records+=("$logical_name$(printf '\t')$(display_path "$entry")")
done

for entry in "${directory_entries[@]}"; do
    logical_name="${entry##*/}"
    logical_names+=("$logical_name")
    logical_records+=("$logical_name$(printf '\t')$(display_path "$entry")/SKILL.md")
done

duplicate_names=()
if ((${#logical_names[@]} > 0)); then
    mapfile -t duplicate_names < <(
        printf '%s\n' "${logical_names[@]}" | LC_ALL=C sort | uniq -d
    )
fi

printf 'Role inventory root: %s\n' "$display_root"
printf 'Flat role documents: %d\n' "${#flat_entries[@]}"
printf 'Directory-style role entry points: %d\n' "${#directory_entries[@]}"
printf 'Total roles: %d\n' "$(( ${#flat_entries[@]} + ${#directory_entries[@]} ))"

printf '\nFlat entry points (%d):\n' "${#flat_entries[@]}"
if ((${#flat_entries[@]} == 0)); then
    printf -- '- none\n'
else
    for entry in "${flat_entries[@]}"; do
        printf -- '- %s\n' "$(display_path "$entry")"
    done
fi

printf '\nDirectory-style entry points (%d):\n' "${#directory_entries[@]}"
if ((${#directory_entries[@]} == 0)); then
    printf -- '- none\n'
else
    for entry in "${directory_entries[@]}"; do
        printf -- '- %s/SKILL.md\n' "$(display_path "$entry")"
    done
fi

printf '\nUnexpected direct entries (%d):\n' "${#unexpected_entries[@]}"
if ((${#unexpected_entries[@]} == 0)); then
    printf -- '- none\n'
else
    for entry in "${unexpected_entries[@]}"; do
        printf -- '- %s\n' "$(display_path "$entry")"
    done
fi

printf '\nMissing entry points (%d):\n' "${#missing_entries[@]}"
if ((${#missing_entries[@]} == 0)); then
    printf -- '- none\n'
else
    for entry in "${missing_entries[@]}"; do
        printf -- '- %s\n' "$(display_path "$entry")"
    done
fi

printf '\nDuplicate logical names (%d):\n' "${#duplicate_names[@]}"
if ((${#duplicate_names[@]} == 0)); then
    printf -- '- none\n'
else
    for logical_name in "${duplicate_names[@]}"; do
        printf -- '- %s\n' "$logical_name"
        duplicate_paths=()
        for record in "${logical_records[@]}"; do
            record_name="${record%%$'\t'*}"
            if [[ "$record_name" == "$logical_name" ]]; then
                duplicate_paths+=("${record#*$'\t'}")
            fi
        done
        while IFS= read -r entry; do
            printf '  %s\n' "$entry"
        done < <(printf '%s\n' "${duplicate_paths[@]}" | LC_ALL=C sort)
    done
fi

actionable_failures=()
for entry in "${unexpected_entries[@]}"; do
    actionable_failures+=("[UNEXPECTED_ENTRY] $(display_path "$entry") is not a direct .md role document or a directory with a direct SKILL.md entry point; remove it or add the required entry point.")
done
for entry in "${missing_entries[@]}"; do
    actionable_failures+=("[MISSING_ENTRY_POINT] $(display_path "$entry") is required by its direct role directory; add the file or remove the directory.")
done
for logical_name in "${duplicate_names[@]}"; do
    duplicate_paths=()
    for record in "${logical_records[@]}"; do
        record_name="${record%%$'\t'*}"
        if [[ "$record_name" == "$logical_name" ]]; then
            duplicate_paths+=("${record#*$'\t'}")
        fi
    done
    duplicate_paths_sorted=""
    while IFS= read -r duplicate_path; do
        if [[ -n "$duplicate_paths_sorted" ]]; then
            duplicate_paths_sorted+=", "
        fi
        duplicate_paths_sorted+="$duplicate_path"
    done < <(printf '%s\n' "${duplicate_paths[@]}" | LC_ALL=C sort)
    actionable_failures+=("[DUPLICATE_LOGICAL_NAME] Logical role name '$logical_name' is declared by $duplicate_paths_sorted; rename one path so each logical name is unique.")
done

printf '\nActionable failures (%d):\n' "${#actionable_failures[@]}"
if ((${#actionable_failures[@]} == 0)); then
    printf -- '- none\n'
    printf '\nValidation result: PASS\n'
    exit 0
fi

for failure in "${actionable_failures[@]}"; do
    printf -- '- %s\n' "$failure"
done
printf '\nValidation result: FAIL\n'
exit 1
