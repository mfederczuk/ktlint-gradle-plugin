#!/bin/sh
# -*- sh -*-
# vim: syntax=sh
# code: language=shellscript

# Copyright (c) 2023 Michael Federczuk
# SPDX-License-Identifier: MPL-2.0 AND Apache-2.0

# <https://github.com/pinterest/ktlint> custom pre-commit hook for Windows
# Generated at //GENERATED_DATETIME::comment_text//
# DO NOT EDIT!

#region preamble

case "$-" in
	(*'i'*)
		\command printf 'script was called interactively\n' >&2
		return 124
		;;
esac

set -o errexit
set -o nounset

# enabling POSIX-compliant behavior for GNU programs
export POSIXLY_CORRECT=yes POSIX_ME_HARDER=yes

# Windows-Changed: no checking for absolute pathname
argv0="$0"
readonly argv0

if [ $# -gt 0 ]; then
	printf '%s: too many arguments: %i\n' "$argv0" $# >&2
	exit 4
fi

#endregion

#region setting up temporary directory

# BEGIN Windows-Changed:
#  * order of environment variables
#  * final fallback is more complicated (includes username)
#  * slashes to backslashes
#  * different filename; converted kebab-case to PascalCase
base_tmp_dir_pathname="${TEMP:-"${TMP:-"${TMPDIR:-"${TEMPDIR:-"${TMP_DIR:-"${TEMP_DIR-}"}"}"}"}"}"
base_tmp_dir_pathname="${base_tmp_dir_pathname%"\\"}"

if [ -z "$base_tmp_dir_pathname" ]; then
	username="$(whoami)"

	base_tmp_dir_pathname="C:\\Users\\$username\\AppData\\Local\\Temp"

	unset -v username
fi

readonly base_tmp_dir_pathname


process_tmp_dir_pathname="$base_tmp_dir_pathname\\KtlintGradlePlugin$$"
# END Windows-Changed
readonly process_tmp_dir_pathname

remove_process_tmp_dir() {
	# shellcheck disable=2317
	rm -r -- "$process_tmp_dir_pathname"
}

mkdir -p -- "$process_tmp_dir_pathname"

trap remove_process_tmp_dir EXIT
trap 'trap - EXIT; remove_process_tmp_dir' INT QUIT TERM

#endregion

#region functions

git() {
	command git -c diff.noprefix=false --no-pager "$@"
}

git_apply() {
	git apply --ignore-whitespace --whitespace=nowarn --allow-empty "$@"
}

# Windows-Changed: redirecting usages of `java` to `javaw`
java() {
	javaw "$@"
}

#endregion

#region generating unstaged changes patch

# Windows-Changed: slash to backslash & different filename; converted kebab-case to PascalCase
unstaged_changes_patch_file_pathname="$process_tmp_dir_pathname\\UnstagedChanges.patch"
readonly unstaged_changes_patch_file_pathname

git diff --patch --raw -z --color=never --full-index --binary \
         --output="$unstaged_changes_patch_file_pathname"

#endregion

#region listing staged kotlin files

# Windows-Changed: slash to backslash & different filename; converted kebab-case to PascalCase and added `dat` extension
staged_kotlin_filename_list_file_pathname="$process_tmp_dir_pathname\\StagedKotlinFiles.dat"
readonly staged_kotlin_filename_list_file_pathname

git diff --name-only -z --color=never --cached --relative \
         --output="$staged_kotlin_filename_list_file_pathname" \
         -- '*.kt' '*.kts'

#endregion

if [ ! -s "$staged_kotlin_filename_list_file_pathname" ]; then
	printf 'No staged Kotlin files; skipping ktlint-check\n' >&2
	exit 0
fi

ktlint_classpath=//KTLINT_CLASSPATH::quoted_string//
readonly ktlint_classpath

ktlint_main_class_name=//KTLINT_MAIN_CLASS_NAME::quoted_string//
readonly ktlint_main_class_name

#region ktlint dry run

if ! java --dry-run -classpath "$ktlint_classpath" "$ktlint_main_class_name"; then
	{
		printf '%s: failed to execute ktlint\n' "$argv0"
		# shellcheck disable=2016
		printf '%s: running the Gradle sync and/or the Gradle task `%s` may fix it\n' "$argv0" //HOOK_INSTALLATION_TASK_NAME::quoted_string//
	} >&2
	exit 48
fi

#endregion

#region detecting IntelliJ IDEA terminal

using_intellij_idea_terminal=false

if [ "${TERM-}" = 'dumb' ]; then
	# when executed via IntelliJ IDEA, $TERM has the value "dumb"
	using_intellij_idea_terminal=true
fi

readonly using_intellij_idea_terminal

#endregion

#region detecting stdin color support

stdin_supports_color=false

if [ -z "${NO_COLOR-}" ] && [ -t 1 ]; then
	case "${TERM-}" in
		('xterm-color'|*'-256color'|'xterm-kitty')
			stdin_supports_color=true
			;;
	esac

	if ! $stdin_supports_color && command -v tput > '/dev/null' && tput 'setaf' '1' 1> '/dev/null' 2>&1; then
		stdin_supports_color=true
	fi
fi

readonly stdin_supports_color

#endregion

#region running ktlint

ktlint_color_opt_arg=''
if $stdin_supports_color; then
	ktlint_color_opt_arg='--color'
fi
readonly ktlint_color_opt_arg

ktlint_code_style_opt_arg=//KTLINT_CODE_STYLE_OPT_ARG::quoted_string//
readonly ktlint_code_style_opt_arg

ktlint_relative_opt_arg='--relative'
if $using_intellij_idea_terminal; then
	# print absolute pathnames if executed via IntelliJ IDEA so that they become clickable
	ktlint_relative_opt_arg=''
fi
readonly ktlint_relative_opt_arg

ktlint_limit_opt_arg=//KTLINT_LIMIT_OPT_ARG::quoted_string//
readonly ktlint_limit_opt_arg

ktlint_experimental_opt_arg=//KTLINT_EXPERIMENTAL_OPT_ARG::quoted_string//
readonly ktlint_experimental_opt_arg

git_apply --reverse -- "$unstaged_changes_patch_file_pathname"

printf 'Running ktlint (v%s)...\n' //KTLINT_VERSION::quoted_string// >&2

exc=0
java -classpath "$ktlint_classpath" "$ktlint_main_class_name" \
     $ktlint_color_opt_arg \
     $ktlint_code_style_opt_arg \
     $ktlint_relative_opt_arg \
     $ktlint_limit_opt_arg \
     $ktlint_experimental_opt_arg \
     --patterns-from-stdin='' < "$staged_kotlin_filename_list_file_pathname" ||
	exc=$?

if [ $exc -eq 0 ]; then
	printf 'Done - no problems found.\n' >&2
fi

git_apply -- "$unstaged_changes_patch_file_pathname"

exit $exc

#endregion
